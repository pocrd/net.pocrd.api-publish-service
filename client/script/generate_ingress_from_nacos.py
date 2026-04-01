#!/usr/bin/env python3
"""
从 Nacos 获取 Dubbo 服务并生成 Higress Ingress 配置
支持为每个服务接口生成对应的 McpBridge 和 Ingress 资源
"""

import sys
import json
import argparse
import urllib.parse
import urllib.request
from typing import Optional, Dict, Any, List
from datetime import datetime

# 颜色定义
class Colors:
    BLUE = '\033[0;34m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    RED = '\033[0;31m'
    NC = '\033[0m'

def get_service_list(nacos_url: str, namespace_id: str = "", group_name: str = "") -> List[Dict[str, Any]]:
    """获取 Nacos 服务列表"""
    params = {
        'pageNo': '1',
        'pageSize': '100'
    }
    if namespace_id:
        params['namespaceId'] = namespace_id
    if group_name:
        params['groupName'] = group_name
    
    url = f"{nacos_url}/nacos/v1/ns/service/list?{urllib.parse.urlencode(params)}"
    
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            content = response.read().decode('utf-8')
            data = json.loads(content)
            return data.get('doms', [])
    except Exception as e:
        print(f"{Colors.RED}✗ 获取服务列表失败：{e}{Colors.NC}", file=sys.stderr)
        return []

def get_service_instances(nacos_url: str, service_name: str, group_name: str = "") -> List[Dict[str, Any]]:
    """获取服务的实例列表"""
    params = {
        'serviceName': service_name
    }
    if group_name:
        params['groupName'] = group_name
    
    url = f"{nacos_url}/nacos/v1/ns/instance/list?{urllib.parse.urlencode(params)}"
    
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            content = response.read().decode('utf-8')
            data = json.loads(content)
            return data.get('hosts', [])
    except Exception as e:
        print(f"{Colors.RED}✗ 获取实例列表失败 [{service_name}]: {e}{Colors.NC}", file=sys.stderr)
        return []

def extract_dubbo_info(instance: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """从实例元数据中提取 Dubbo 服务信息"""
    metadata = instance.get('metadata', {})
    
    # 检查是否是 Dubbo 服务
    if metadata.get('dubbo') != '2.0.2':
        return None
    
    interface = metadata.get('interface', '')
    version = metadata.get('version', '')
    revision = metadata.get('revision', '')
    group = metadata.get('group', '')
    application = metadata.get('application', '')
    
    if not all([interface, version, application]):
        return None
    
    # 获取协议和端口
    protocol = metadata.get('protocol', 'dubbo')
    port = instance.get('port', 20880)
    ip = instance.get('ip', '')
    
    return {
        'interface': interface,
        'version': version,
        'revision': revision,
        'group': group,
        'application': application,
        'protocol': protocol,
        'ip': ip,
        'port': port,
        'service_name': f"{interface}:{version}",
        'enabled': instance.get('enabled', True),
        'healthy': instance.get('healthy', True)
    }

def generate_mcp_bridge(dubbo_services: List[Dict[str, Any]]) -> str:
    """生成 McpBridge 配置"""
    # 按应用分组
    apps = {}
    for svc in dubbo_services:
        app_name = svc['application']
        if app_name not in apps:
            apps[app_name] = {
                'name': app_name,
                'services': []
            }
        apps[app_name]['services'].append(svc)
    
    mcp_bridges = []
    for app_name, app_data in apps.items():
        services_config = []
        for svc in app_data['services']:
            # 生成服务配置
            service_config = {
                'name': svc['interface'].split('.')[-1],  # 使用接口名最后一部分
                'interface': svc['interface'],
                'version': svc['version'],
                'group': svc['group'] if svc['group'] else 'default'
            }
            services_config.append(service_config)
        
        mcp_bridge = {
            'apiVersion': 'networking.higress.io/v1',
            'kind': 'McpBridge',
            'metadata': {
                'name': app_name,
                'namespace': 'higress-system'
            },
            'spec': {
                'registries': [
                    {
                        'name': f'{app_name}-nacos',
                        'type': 'nacos',
                        'domain': 'nacos-server.higress-system.svc.cluster.local',
                        'port': 30848,
                        'nacosNamespaceId': 'public',
                        'nacosGroups': [svc['group'] if svc['group'] else 'DEFAULT_GROUP']
                    }
                ]
            }
        }
        mcp_bridges.append(mcp_bridge)
    
    return mcp_bridges

def generate_ingress(dubbo_services: List[Dict[str, Any]], domain: str = "api.example.com") -> List[Dict[str, Any]]:
    """生成 Ingress 配置"""
    ingresses = []
    
    for svc in dubbo_services:
        # 从接口名生成路径
        interface_parts = svc['interface'].split('.')
        service_name = interface_parts[-1]
        package_path = '/'.join(interface_parts[:-1])
        
        # 生成路径前缀
        path_prefix = f"/dubbo/{package_path}/{service_name}"
        
        ingress = {
            'apiVersion': 'networking.k8s.io/v1',
            'kind': 'Ingress',
            'metadata': {
                'name': f'{svc["application"]}-{service_name.lower()}',
                'namespace': 'higress-system',
                'annotations': {
                    'higress.io/destination': f'{svc["application"]}-{svc["interface"]}:{svc["version"]}',
                    'higress.io/rpc-type': 'dubbo',
                    'higress.io/dubbo-version': svc['version'],
                    'higress.io/dubbo-group': svc['group'] if svc['group'] else 'default'
                }
            },
            'spec': {
                'ingressClassName': 'higress',
                'rules': [
                    {
                        'host': domain,
                        'http': {
                            'paths': [
                                {
                                    'path': path_prefix,
                                    'pathType': 'Prefix',
                                    'backend': {
                                        'service': {
                                            'name': svc['application'],
                                            'port': {
                                                'number': 80
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
        }
        ingresses.append(ingress)
    
    return ingresses

def generate_envoy_filter(dubbo_services: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
    """生成 EnvoyFilter 配置（用于 Dubbo 协议转换）"""
    if not dubbo_services:
        return None
    
    # 按应用分组
    apps = {}
    for svc in dubbo_services:
        app_name = svc['application']
        if app_name not in apps:
            apps[app_name] = []
        apps[app_name].append(svc)
    
    envoy_filters = []
    for app_name, services in apps.items():
        envoy_filter = {
            'apiVersion': 'networking.istio.io/v1alpha3',
            'kind': 'EnvoyFilter',
            'metadata': {
                'name': f'{app_name}-dubbo-filter',
                'namespace': 'higress-system'
            },
            'spec': {
                'configPatches': [
                    {
                        'applyTo': 'HTTP_FILTER',
                        'match': {
                            'context': 'GATEWAY',
                            'listener': {
                                'filterChain': {
                                    'filter': {
                                        'name': 'envoy.filters.network.http_connection_manager'
                                    }
                                }
                            }
                        },
                        'patch': {
                            'operation': 'INSERT_BEFORE',
                            'value': {
                                'name': 'envoy.filters.http.dubbo_proxy',
                                'typed_config': {
                                    '@type': 'type.googleapis.com/envoy.extensions.filters.http.dubbo_proxy.v3.DubboProxyConfig',
                                    'stat_prefix': f'{app_name}_dubbo',
                                    'protocol_type': 'Dubbo',
                                    'serialization_type': 'Hessian2'
                                }
                            }
                        }
                    }
                ]
            }
        }
        envoy_filters.append(envoy_filter)
    
    return envoy_filters

def main():
    parser = argparse.ArgumentParser(
        description='从 Nacos 获取 Dubbo 服务并生成 Higress Ingress 配置',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
    # 生成所有服务的配置
    python3 generate_ingress_from_nacos.py -n http://localhost:8848
    
    # 指定域名生成配置
    python3 generate_ingress_from_nacos.py -n http://localhost:8848 -d api.caringfamily.cn
    
    # 只生成 McpBridge
    python3 generate_ingress_from_nacos.py -n http://localhost:8848 --mcp-only
    
    # 输出到文件
    python3 generate_ingress_from_nacos.py -n http://localhost:8848 -o ingress.yaml
        '''
    )
    
    parser.add_argument('-n', '--nacos', required=True,
                        help='Nacos 服务器地址 (如: http://localhost:8848)')
    parser.add_argument('-d', '--domain', default='api.example.com',
                        help='Ingress 域名 (默认: api.example.com)')
    parser.add_argument('-g', '--group', default='',
                        help='Nacos 服务分组 (默认: 空)')
    parser.add_argument('--namespace', default='',
                        help='Nacos 命名空间 ID (默认: 空)')
    parser.add_argument('-o', '--output',
                        help='输出文件路径 (默认: 输出到 stdout)')
    parser.add_argument('--mcp-only', action='store_true',
                        help='只生成 McpBridge 配置')
    parser.add_argument('--ingress-only', action='store_true',
                        help='只生成 Ingress 配置')
    parser.add_argument('--format', choices=['yaml', 'json'], default='yaml',
                        help='输出格式 (默认: yaml)')
    
    args = parser.parse_args()
    
    print(f"{Colors.BLUE}正在从 Nacos 获取 Dubbo 服务...{Colors.NC}")
    print(f"  Nacos: {args.nacos}")
    if args.group:
        print(f"  Group: {args.group}")
    print()
    
    # 获取服务列表
    services = get_service_list(args.nacos, args.namespace, args.group)
    if not services:
        print(f"{Colors.YELLOW}⚠ 未找到任何服务{Colors.NC}")
        sys.exit(0)
    
    print(f"{Colors.GREEN}✓ 找到 {len(services)} 个服务{Colors.NC}")
    
    # 收集 Dubbo 服务信息
    dubbo_services = []
    for service_name in services:
        instances = get_service_instances(args.nacos, service_name, args.group)
        for instance in instances:
            dubbo_info = extract_dubbo_info(instance)
            if dubbo_info:
                dubbo_services.append(dubbo_info)
                print(f"  {Colors.GREEN}•{Colors.NC} {dubbo_info['interface']}:{dubbo_info['version']}")
    
    if not dubbo_services:
        print(f"{Colors.YELLOW}⚠ 未找到 Dubbo 服务{Colors.NC}")
        sys.exit(0)
    
    print(f"\n{Colors.GREEN}✓ 共找到 {len(dubbo_services)} 个 Dubbo 服务{Colors.NC}")
    
    # 生成配置
    all_configs = []
    
    if not args.ingress_only:
        print(f"\n{Colors.BLUE}生成 McpBridge 配置...{Colors.NC}")
        mcp_bridges = generate_mcp_bridge(dubbo_services)
        all_configs.extend(mcp_bridges)
        print(f"  {Colors.GREEN}✓ 生成 {len(mcp_bridges)} 个 McpBridge{Colors.NC}")
    
    if not args.mcp_only:
        print(f"\n{Colors.BLUE}生成 Ingress 配置...{Colors.NC}")
        ingresses = generate_ingress(dubbo_services, args.domain)
        all_configs.extend(ingresses)
        print(f"  {Colors.GREEN}✓ 生成 {len(ingresses)} 个 Ingress{Colors.NC}")
    
    # 输出配置
    output = []
    if args.format == 'yaml':
        try:
            import yaml
            # 添加注释头
            output.append(f"# Generated by generate_ingress_from_nacos.py")
            output.append(f"# Date: {datetime.now().isoformat()}")
            output.append(f"# Nacos: {args.nacos}")
            output.append(f"# Total Services: {len(dubbo_services)}")
            output.append("")
            
            for config in all_configs:
                output.append(yaml.dump(config, default_flow_style=False, allow_unicode=True))
                output.append("---\n")
            
            result = '\n'.join(output)
        except ImportError:
            print(f"{Colors.YELLOW}⚠ 未安装 PyYAML，使用 JSON 格式输出{Colors.NC}")
            result = json.dumps(all_configs, indent=2, ensure_ascii=False)
    else:
        result = json.dumps(all_configs, indent=2, ensure_ascii=False)
    
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(result)
        print(f"\n{Colors.GREEN}✓ 配置已保存到: {args.output}{Colors.NC}")
    else:
        print(f"\n{Colors.YELLOW}========== 生成的配置 =========={Colors.NC}\n")
        print(result)

if __name__ == '__main__':
    main()
