#!/usr/bin/env python3
"""
从 Nacos 获取并解码 API 元数据的 Python 脚本
支持遍历所有 Dubbo 配置或指定单个服务
"""

import sys
import json
import gzip
import base64
import urllib.parse
import urllib.request
from typing import Optional, Dict, Any, List, Tuple

# 颜色定义
class Colors:
    BLUE = '\033[0;34m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    RED = '\033[0;31m'
    NC = '\033[0m'  # No Color

def get_config(nacos_url: str, data_id: str, group: str) -> Optional[Dict[str, Any]]:
    """从 Nacos 获取配置"""
    params = {
        'dataId': data_id,
        'group': group
    }
    
    url = f"{nacos_url}/nacos/v1/cs/configs?{urllib.parse.urlencode(params)}"
    
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            content = response.read().decode('utf-8')
            return json.loads(content)
    except Exception as e:
        print(f"{Colors.RED}✗ 获取失败：{e}{Colors.NC}", file=sys.stderr)
        return None

def decode_metadata(compressed_base64: str) -> Optional[Dict[str, Any]]:
    """解码 Base64+GZIP 压缩的元数据"""
    try:
        decoded = base64.b64decode(compressed_base64)
        decompressed = gzip.decompress(decoded)
        return json.loads(decompressed.decode('utf-8'))
    except Exception as e:
        print(f"{Colors.RED}解码失败：{e}{Colors.NC}", file=sys.stderr)
        return None

def get_service_instances(nacos_url: str, service_name: str, group_name: str) -> List[Dict[str, Any]]:
    """获取服务的实例列表"""
    params = {
        'serviceName': service_name,
        'groupName': group_name
    }
    url = f"{nacos_url}/nacos/v1/ns/instance/list?{urllib.parse.urlencode(params)}"
    
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            content = response.read().decode('utf-8')
            data = json.loads(content)
            return data.get('hosts', [])
    except Exception as e:
        print(f"{Colors.RED}获取实例列表失败 [{service_name}]: {e}{Colors.NC}", file=sys.stderr)
        return []


def extract_dubbo_info_from_instance(instance: Dict[str, Any]) -> Optional[Dict[str, str]]:
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
    
    if not all([interface, version, revision, application]):
        return None
    
    # 提取 api.md5.{interface} 元数据（如果存在）
    api_md5_key = f"api.md5.{interface}"
    api_md5 = metadata.get(api_md5_key, '')
    
    return {
        'interface': interface,
        'version': version,
        'revision': revision,
        'group': group,
        'application': application,
        'api_md5': api_md5,
        'ip': instance.get('ip', ''),
        'port': instance.get('port', '')
    }


def build_data_id(dubbo_info: Dict[str, str]) -> str:
    """根据 Dubbo 信息构建 dataId
    
    格式：{interface}:{version}-{api_md5}:{group}:provider:{application}
    例如：com.pocrd.api_publish_service.api.GreeterServiceHttpExport:1.0.0-0b88d866c7e3e08b:public:provider:api-publish-service
    
    如果存在 api.md5 元数据，则使用 version-md5 格式；否则使用普通 version 格式
    """
    group = dubbo_info['group']
    interface = dubbo_info['interface']
    version = dubbo_info['version']
    application = dubbo_info['application']
    api_md5 = dubbo_info.get('api_md5', '')
    
    # 如果存在 api.md5，使用 version-md5 格式
    if api_md5:
        version_with_md5 = f"{version}-{api_md5}"
    else:
        version_with_md5 = version
    
    # 构建 dataId: {interface}:{version_with_md5}:{group}:provider:{application}
    return f"{interface}:{version_with_md5}:{group}:provider:{application}"

def parse_service_info(data_id: str) -> Optional[Tuple[str, str, str]]:
    """从 dataId 解析服务信息
    
    格式：group-interface:version:version:group:provider:application
    例如：public-com.pocrd.api_publish_service.api.GreeterServiceHttpExport:1.0.0:1.0.0:public:provider:api-publish-service
    """
    parts = data_id.split(':')
    if len(parts) != 6:
        return None
    
    # 提取 group 和 interface
    group_interface = parts[0]
    if '-' not in group_interface:
        return None
    
    group, interface = group_interface.split('-', 1)
    version = parts[1]
    
    return (interface, version, group)

def show_service_metadata(nacos_url: str, data_id: str, service_interface: str, version: str, group: str) -> bool:
    """显示指定服务的元数据"""
    print(f"\n{Colors.BLUE}{'='*60}{Colors.NC}")
    print(f"{Colors.BLUE}服务：{service_interface}{Colors.NC}")
    print(f"{Colors.BLUE}版本：{version}{Colors.NC}")
    print(f"{Colors.BLUE}分组：{group}{Colors.NC}")
    print(f"{Colors.BLUE}DataId：{data_id}{Colors.NC}")
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
    
    # 获取配置
    config = get_config(nacos_url, data_id, "dubbo")
    
    if config is None:
        return False
    
    # 提取 parameters 字段中的 api.metadata（SDK 增强的元数据）
    parameters = config.get('parameters', {})
    compressed_data = parameters.get('api.metadata', '')
    
    if not compressed_data:
        # 尝试旧版本的字段名
        compressed_data = parameters.get('api.publish.service.metadata', '')
    
    if not compressed_data:
        print(f"{Colors.YELLOW}⚠ 未找到 'api.metadata' 字段（可能是 Dubbo 框架服务或无 SDK 注解）{Colors.NC}")
        return False
    
    print(f"{Colors.YELLOW}压缩的元数据大小：{len(compressed_data)} bytes{Colors.NC}")
    print(f"\n{Colors.GREEN}解码后的元数据：{Colors.NC}\n")
    
    # 解码并格式化输出
    metadata = decode_metadata(compressed_data)
    
    if metadata:
        print(json.dumps(metadata, indent=2, ensure_ascii=False))
        
        # 打印摘要信息
        print(f"\n{Colors.YELLOW}摘要：{Colors.NC}")
        services = metadata.get('services', [])
        entities = metadata.get('entities', {})
        error_codes = metadata.get('errorCodes', [])
        
        if services:
            print(f"  - 服务数量：{len(services)}")
            for svc in services:
                methods = svc.get('methods', [])
                print(f"    - {svc.get('interfaceName', 'Unknown')}: {len(methods)} 个方法")
        
        if entities:
            print(f"  - 实体类型数量：{len(entities)}")
        
        if error_codes:
            total_codes = sum(len(ec.get('codes', [])) for ec in error_codes)
            print(f"  - 错误码数量：{total_codes}")
            for ec in error_codes:
                for code in ec.get('codes', [])[:3]:  # 只显示前3个
                    print(f"      {code['code']}: {code['name']} - {code['desc']}")
                if len(ec.get('codes', [])) > 3:
                    print(f"      ... 还有 {len(ec.get('codes', [])) - 3} 个错误码")
        
        return True
    else:
        return False

def list_nacos_services(nacos_url: str, group_name: str = "PUBLIC-GROUP") -> List[Dict[str, Any]]:
    """从 Nacos 获取服务列表"""
    url = f"{nacos_url}/nacos/v1/ns/service/list?groupName={group_name}&pageNo=1&pageSize=100"
    
    try:
        with urllib.request.urlopen(url, timeout=10) as response:
            content = response.read().decode('utf-8')
            data = json.loads(content)
            
            count = data.get('count', 0)
            services = data.get('doms', [])
            
            return [{"name": svc, "group": group_name} for svc in services]
            
    except Exception as e:
        print(f"{Colors.RED}获取服务列表失败：{e}{Colors.NC}", file=sys.stderr)
        return []


def print_service_list(services: List[Dict[str, Any]], title: str = "Nacos 服务列表"):
    """打印服务列表"""
    print(f"\n{Colors.BLUE}{'='*60}{Colors.NC}")
    print(f"{Colors.BLUE}{title}{Colors.NC}")
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
    
    if not services:
        print(f"{Colors.YELLOW}  暂无服务{Colors.NC}")
        return
    
    for i, svc in enumerate(services, 1):
        print(f"  {i}. {Colors.GREEN}{svc['name']}{Colors.NC} (group: {svc['group']})")
    
    print(f"\n{Colors.YELLOW}总计：{len(services)} 个服务{Colors.NC}")


def collect_api_md5_from_service(nacos_url: str, service_name: str, group_name: str) -> Dict[str, str]:
    """从服务实例中提取 api.md5 元数据"""
    api_md5_map = {}
    
    try:
        instances = get_service_instances(nacos_url, service_name, group_name)
        for instance in instances:
            metadata = instance.get('metadata', {})
            # 提取所有 api.md5.{interface} 键
            for key, value in metadata.items():
                if key.startswith('api.md5.'):
                    interface = key.replace('api.md5.', '')
                    api_md5_map[interface] = value
    except Exception:
        pass
    
    return api_md5_map


def collect_dubbo_services_from_instances(nacos_url: str, services: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """从服务实例中收集 Dubbo 服务信息"""
    dubbo_services = []
    seen_data_ids = set()
    
    # 预先收集应用级服务的 api.md5
    app_service_md5 = {}
    for svc in services:
        service_name = svc['name']
        group_name = svc['group']
        # 跳过 providers: 开头的服务
        if service_name.startswith('providers:'):
            continue
        md5_map = collect_api_md5_from_service(nacos_url, service_name, group_name)
        if md5_map:
            app_service_md5[service_name] = md5_map
    
    print(f"{Colors.YELLOW}  发现 {len(app_service_md5)} 个应用包含 api.md5{Colors.NC}")
    
    for svc in services:
        service_name = svc['name']
        group_name = svc['group']
        
        # 跳过非 provider 服务
        if not service_name.startswith('providers:'):
            continue
        
        # 获取服务实例
        instances = get_service_instances(nacos_url, service_name, group_name)
        
        if not instances:
            continue
        
        # 从第一个实例中提取 Dubbo 信息（假设同一服务的实例信息相同）
        for instance in instances:
            dubbo_info = extract_dubbo_info_from_instance(instance)
            if dubbo_info:
                interface = dubbo_info.get('interface', '')
                
                # 检查是否有对应的 api.md5（从应用级服务中获取）
                application = dubbo_info.get('application', '')
                api_md5 = ''
                if application in app_service_md5:
                    api_md5 = app_service_md5[application].get(interface, '')
                    
                # 如果没有 api.md5，跳过该服务（只在有 api.md5 时才查询元数据）
                if not api_md5:
                    break
                
                # 更新 dubbo_info 中的 api_md5
                dubbo_info['api_md5'] = api_md5
                
                # 构建 dataId
                data_id = build_data_id(dubbo_info)
                
                # 去重
                if data_id in seen_data_ids:
                    continue
                seen_data_ids.add(data_id)
                dubbo_services.append({
                    'data_id': data_id,
                    'service_name': service_name,
                    'group_name': group_name,
                    'dubbo_info': dubbo_info,
                    'instance': instance
                })
                break
    
    return dubbo_services


def show_all_dubbo_metadata(nacos_url: str):
    """遍历并显示所有 Dubbo 服务的元数据"""
    print(f"{Colors.YELLOW}正在获取 Nacos 中所有 Dubbo 配置...{Colors.NC}\n")
    
    # 第一步：获取并打印 Nacos 服务列表
    print(f"{Colors.YELLOW}步骤 1/3: 检索 Nacos 服务列表...{Colors.NC}")
    
    public_services = list_nacos_services(nacos_url, "PUBLIC-GROUP")
    internal_services = list_nacos_services(nacos_url, "INTERNAL-GROUP")
    all_nacos_services = public_services + internal_services
    
    print_service_list(public_services, "PUBLIC-GROUP 服务列表")
    print_service_list(internal_services, "INTERNAL-GROUP 服务列表")
    
    print(f"\n{Colors.GREEN}✓ 共发现 {len(all_nacos_services)} 个 Nacos 服务{Colors.NC}\n")
    
    # 第二步：从服务实例中提取 Dubbo 信息
    print(f"{Colors.YELLOW}步骤 2/3: 从服务实例中提取 Dubbo 信息...{Colors.NC}")
    
    dubbo_services = collect_dubbo_services_from_instances(nacos_url, all_nacos_services)
    
    if not dubbo_services:
        print(f"{Colors.RED}未找到任何 Dubbo 服务实例{Colors.NC}")
        return
    
    print(f"{Colors.GREEN}找到 {len(dubbo_services)} 个 Dubbo 服务（已去重）{Colors.NC}\n")
    
    # 区分业务服务和框架服务
    business_services = []
    framework_services = []
    
    for svc in dubbo_services:
        interface = svc['dubbo_info']['interface']
        if interface.startswith('org.apache.dubbo.'):
            framework_services.append(svc)
        else:
            business_services.append(svc)
    
    print(f"{Colors.GREEN}其中 {len(business_services)} 个业务服务，{len(framework_services)} 个框架服务{Colors.NC}\n")
    
    # 打印发现的 Dubbo 服务摘要
    print(f"\n{Colors.BLUE}{'='*60}{Colors.NC}")
    print(f"{Colors.BLUE}发现的 Dubbo 服务摘要{Colors.NC}")
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
    for i, svc in enumerate(business_services, 1):
        info = svc['dubbo_info']
        print(f"  {i}. {Colors.GREEN}{info['interface']}{Colors.NC}")
        print(f"     版本: {info['version']}, 分组: {info['group']}, 应用: {info['application']}")
        print(f"     DataId: {Colors.YELLOW}{svc['data_id']}{Colors.NC}")
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}\n")
    
    # 第三步：解码并显示业务服务元数据
    if business_services:
        print(f"{Colors.YELLOW}步骤 3/3: 解码业务服务接口元数据...{Colors.NC}")
        
        success_count = 0
        for svc in business_services:
            info = svc['dubbo_info']
            if show_service_metadata(nacos_url, svc['data_id'], info['interface'], 
                                     info['version'], info['group']):
                success_count += 1
        
        print(f"\n{Colors.BLUE}{'='*60}{Colors.NC}")
        print(f"{Colors.BLUE}业务服务总计：{len(business_services)} 个，成功解码 {success_count} 个{Colors.NC}")
        print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
    
    # 再显示框架服务（可选，带过滤）
    if framework_services:
        print(f"\n{Colors.YELLOW}是否显示 {len(framework_services)} 个 Dubbo 框架服务？(y/N): {Colors.NC}", end='')
        try:
            response = input().strip().lower()
            if response == 'y':
                print(f"\n{Colors.BLUE}{'='*60}{Colors.NC}")
                print(f"{Colors.BLUE}Dubbo 框架服务元数据{Colors.NC}")
                print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
                
                for svc in framework_services:
                    info = svc['dubbo_info']
                    show_service_metadata(nacos_url, svc['data_id'], info['interface'],
                                         info['version'], info['group'])
        except EOFError:
            pass  # 非交互式环境，跳过

def show_single_service(nacos_url: str, service_interface: str, version: str, group: str, api_md5: str = ''):
    """显示单个指定服务的元数据"""
    # 构建 dataId（Dubbo 3.3.6 格式，支持 MD5）
    if api_md5:
        version_with_md5 = f"{version}-{api_md5}"
    else:
        version_with_md5 = version
    data_id = f"{group}-{service_interface}:{version}:{version_with_md5}:{group}:provider:api-publish-service"
    show_service_metadata(nacos_url, data_id, service_interface, version, group)

def main():
    # 解析命令行参数
    if len(sys.argv) == 1:
        # 无参数：遍历所有 Dubbo 服务
        nacos_addr = "http://localhost:30848"
        mode = "all"
    elif len(sys.argv) == 2:
        # 1个参数：Nacos 地址，遍历所有
        nacos_addr = sys.argv[1]
        mode = "all"
    elif len(sys.argv) == 5:
        # 4个参数：Nacos 地址 + 服务信息
        nacos_addr = sys.argv[1]
        mode = "single"
        service_interface = sys.argv[2]
        version = sys.argv[3]
        group = sys.argv[4]
    else:
        print(f"用法: {sys.argv[0]} [nacos_url] [service_interface version group]")
        print(f"  {sys.argv[0]}                          # 遍历所有 Dubbo 服务（默认 localhost:30848）")
        print(f"  {sys.argv[0]} http://nacos:8848        # 遍历所有 Dubbo 服务（指定 Nacos）")
        print(f"  {sys.argv[0]} http://nacos:8848 com.example.Service 1.0.0 public  # 获取单个服务")
        sys.exit(1)
    
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
    print(f"{Colors.BLUE}Nacos API 元数据获取与解码工具{Colors.NC}")
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}")
    print(f"{Colors.YELLOW}Nacos 地址：{nacos_addr}{Colors.NC}\n")
    
    # 检查 Nacos 是否可访问
    try:
        urllib.request.urlopen(f"{nacos_addr}/nacos/", timeout=3)
        print(f"{Colors.GREEN}✓ Nacos 连接正常{Colors.NC}\n")
    except Exception as e:
        print(f"{Colors.RED}错误：无法连接到 Nacos ({nacos_addr}){Colors.NC}")
        print("请检查 Nacos 地址是否正确，或服务是否正在运行")
        sys.exit(1)
    
    # 根据模式执行
    if mode == "all":
        show_all_dubbo_metadata(nacos_addr)
    else:
        show_single_service(nacos_addr, service_interface, version, group)
    
    print(f"\n{Colors.BLUE}{'='*60}{Colors.NC}")
    print(f"{Colors.BLUE}完成{Colors.NC}")
    print(f"{Colors.BLUE}{'='*60}{Colors.NC}")

if __name__ == "__main__":
    main()
