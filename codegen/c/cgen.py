#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
C 代码生成工具 - 基于 Jinja2 模板引擎
从 JSON 元数据或 Nacos 生成 C 实体类、API 接口和错误码定义
"""

import json
import os
import re
import gzip
import base64
import argparse
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Dict, List, Any, Optional
from jinja2 import Environment, FileSystemLoader


class NacosMetadataFetcher:
    """Nacos 元数据获取器"""
    
    def __init__(self, nacos_url: str):
        self.nacos_url = nacos_url.rstrip('/')
    
    def get_config(self, data_id: str, group: str = "dubbo") -> Optional[Dict[str, Any]]:
        """从 Nacos 获取配置"""
        params = {'dataId': data_id, 'group': group}
        url = f"{self.nacos_url}/nacos/v1/cs/configs?{urllib.parse.urlencode(params)}"
        
        try:
            with urllib.request.urlopen(url, timeout=10) as response:
                content = response.read().decode('utf-8')
                return json.loads(content)
        except Exception as e:
            print(f"获取配置失败 [{data_id}]: {e}")
            return None
    
    def get_service_instances(self, service_name: str, group_name: str) -> List[Dict[str, Any]]:
        """获取服务的实例列表"""
        params = {'serviceName': service_name, 'groupName': group_name}
        url = f"{self.nacos_url}/nacos/v1/ns/instance/list?{urllib.parse.urlencode(params)}"
        
        try:
            with urllib.request.urlopen(url, timeout=10) as response:
                content = response.read().decode('utf-8')
                data = json.loads(content)
                return data.get('hosts', [])
        except Exception as e:
            print(f"获取实例列表失败 [{service_name}]: {e}")
            return []
    
    def list_services(self, group_name: str = "PUBLIC-GROUP") -> List[Dict[str, Any]]:
        """从 Nacos 获取服务列表"""
        url = f"{self.nacos_url}/nacos/v1/ns/service/list?groupName={group_name}&pageNo=1&pageSize=100"
        
        try:
            with urllib.request.urlopen(url, timeout=10) as response:
                content = response.read().decode('utf-8')
                data = json.loads(content)
                services = data.get('doms', [])
                return [{"name": svc, "group": group_name} for svc in services]
        except Exception as e:
            print(f"获取服务列表失败: {e}")
            return []
    
    def extract_api_md5(self, service_name: str, group_name: str) -> Dict[str, str]:
        """从服务实例中提取 api.md5 元数据"""
        api_md5_map = {}
        try:
            instances = self.get_service_instances(service_name, group_name)
            for instance in instances:
                metadata = instance.get('metadata', {})
                for key, value in metadata.items():
                    if key.startswith('api.md5.'):
                        interface_with_version = key.replace('api.md5.', '')
                        api_md5_map[interface_with_version] = value
        except Exception:
            pass
        return api_md5_map
    
    def decode_metadata(self, compressed_base64: str) -> Optional[Dict[str, Any]]:
        """解码 Base64+GZIP 压缩的元数据"""
        try:
            decoded = base64.b64decode(compressed_base64)
            decompressed = gzip.decompress(decoded)
            return json.loads(decompressed.decode('utf-8'))
        except Exception as e:
            print(f"解码元数据失败: {e}")
            return None
    
    def fetch_service_metadata(self, interface: str, version: str, group: str, 
                               application: str, api_md5: str = "") -> Optional[Dict[str, Any]]:
        """获取指定服务的元数据"""
        if api_md5:
            version_with_md5 = f"{version}-{api_md5}"
        else:
            version_with_md5 = version
        data_id = f"{interface}:{version_with_md5}:{group}:provider:{application}"
        
        config = self.get_config(data_id)
        if not config:
            return None
        
        parameters = config.get('parameters', {})
        compressed_data = parameters.get('api.metadata', '')
        
        if not compressed_data:
            compressed_data = parameters.get('api.publish.service.metadata', '')
        
        if not compressed_data:
            print(f"未找到 api.metadata: {interface}")
            return None
        
        return self.decode_metadata(compressed_data)
    
    def fetch_all_services(self) -> List[Dict[str, Any]]:
        """获取所有带有 API 元数据的服务"""
        all_metadata = []
        
        public_services = self.list_services("PUBLIC-GROUP")
        internal_services = self.list_services("INTERNAL-GROUP")
        all_services = public_services + internal_services
        
        print(f"发现 {len(all_services)} 个 Nacos 服务")
        
        app_md5_map = {}
        for svc in all_services:
            if svc['name'].startswith('providers:'):
                continue
            md5_map = self.extract_api_md5(svc['name'], svc['group'])
            if md5_map:
                app_md5_map[svc['name']] = md5_map
        
        print(f"发现 {len(app_md5_map)} 个应用包含 api.md5")
        
        seen = set()
        for svc in all_services:
            if not svc['name'].startswith('providers:'):
                continue
            
            instances = self.get_service_instances(svc['name'], svc['group'])
            if not instances:
                continue
            
            for instance in instances:
                metadata = instance.get('metadata', {})
                if metadata.get('dubbo') != '2.0.2':
                    continue
                
                interface = metadata.get('interface', '')
                version = metadata.get('version', '')
                application = metadata.get('application', '')
                
                if not all([interface, version, application]):
                    continue
                
                api_md5 = ''
                if application in app_md5_map:
                    key = f"{interface}:{version}"
                    api_md5 = app_md5_map[application].get(key, '')
                
                if not api_md5:
                    continue
                
                key = f"{interface}:{version}"
                if key in seen:
                    continue
                seen.add(key)
                
                meta = self.fetch_service_metadata(
                    interface, version, metadata.get('group', ''), 
                    application, api_md5
                )
                
                if meta:
                    all_metadata.append({
                        'interface': interface,
                        'version': version,
                        'application': application,
                        'metadata': meta
                    })
                    print(f"✓ 获取: {interface}")
        
        return all_metadata


class CCodeGenerator:
    """C 代码生成器"""
    
    # 类型映射：JSON 类型 -> C 类型
    TYPE_MAPPING = {
        'string': 'char*',
        'int': 'int',
        'long': 'long long',
        'bool': 'bool',
        'float': 'float',
        'double': 'double',
        'byte': 'unsigned char',
        'short': 'short',
        'char': 'char',
        'void': 'void',
    }
    
    def __init__(self, template_dir: str, output_dir: str, prefix: str = "apg"):
        """
        初始化代码生成器
        
        Args:
            template_dir: Jinja2 模板目录
            output_dir: 代码输出目录
            prefix: C 代码前缀（用于命名空间隔离）
        """
        self.template_dir = Path(template_dir)
        self.output_dir = Path(output_dir)
        self.prefix = prefix
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 初始化 Jinja2 环境
        self.env = Environment(
            loader=FileSystemLoader(str(self.template_dir)),
            autoescape=False,
            trim_blocks=True,
            lstrip_blocks=True,
            keep_trailing_newline=True,
        )
        
        # 注册自定义过滤器
        self.env.filters['to_c_type'] = self.to_c_type
        self.env.filters['to_snake_case'] = self.to_snake_case
        self.env.filters['escape_c_string'] = self.escape_c_string
        self.env.filters['entity_func_prefix'] = self.entity_func_prefix
        
        # 注册模板测试
        self.env.tests['entity_type'] = lambda t: '_' in t and t not in self.TYPE_MAPPING
    
    def to_c_type(self, type_str: str) -> str:
        """将 JSON 类型转换为 C 类型"""
        if not type_str:
            return 'void'
    
        # 处理数组类型（以 ; 结尾表示数组）
        if type_str.endswith(';'):
            base_type = type_str[:-1]
            normalized = self._normalize_type(base_type)
            # 实体类型数组：DubboDemo_Order; -> DubboDemo_Order_t*
            if self._is_entity_type(base_type):
                return f'{normalized}_t*'
            return f'{normalized}*'
    
        # 实体类型：DubboDemo_Order -> DubboDemo_Order_t*
        if self._is_entity_type(type_str):
            return f'{self._normalize_type(type_str)}_t*'
    
        return self._normalize_type(type_str)
    
    def _is_entity_type(self, type_str: str) -> bool:
        """判断是否为实体类型"""
        if not type_str:
            return False
        if type_str in self.TYPE_MAPPING:
            return False
        if '.' in type_str or '_' in type_str:
            return True
        return False

    def entity_func_prefix(self, type_str: str) -> str:
        """获取实体类型对应的函数名前缀，如 DubboDemo_Order -> dubbodemo_order"""
        return self.to_snake_case(self._normalize_type(type_str))
    
    def _normalize_type(self, type_str: str) -> str:
        """规范化类型字符串"""
        if not type_str:
            return type_str
        
        # 内置基础类型
        if type_str in self.TYPE_MAPPING:
            return self.TYPE_MAPPING[type_str]
        
        # 处理错误生成的类型名（如 ApiPublish_String 应该映射为 char*）
        if '_' in type_str:
            parts = type_str.split('_')
            if len(parts) == 2:
                potential_builtin = parts[1].lower()
                if potential_builtin in self.TYPE_MAPPING:
                    return self.TYPE_MAPPING[potential_builtin]
        
        # 全限定名 → 去包名
        if '.' in type_str:
            type_str = type_str.split('.')[-1]
        
        # ServiceId_ClassName 格式 → 保留原样
        return type_str
    
    def to_snake_case(self, name: str) -> str:
        """将类名转换为蛇形命名"""
        # 处理 ServiceId_ClassName 格式
        if '_' in name:
            parts = name.split('_')
            if len(parts) == 2:
                return f"{parts[0].lower()}_{self._pascal_to_snake(parts[1])}"
        
        return self._pascal_to_snake(name)
    
    def _pascal_to_snake(self, name: str) -> str:
        """将帕斯卡命名转换为蛇形命名"""
        # 插入下划线在大写字母前
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
        return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()
    
    def escape_c_string(self, s: str) -> str:
        """转义 C 字符串中的特殊字符"""
        if not s:
            return ''
        return s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t')
    
    def _to_header_guard(self, name: str) -> str:
        """生成头文件保护宏"""
        return f"{self.prefix.upper()}_{name.upper().replace('_', '_')}"
    
    def generate_entities(self, service_data: Dict[str, Any]) -> List[Path]:
        """生成实体类头文件和源文件"""
        template_h = self.env.get_template('entity.h.j2')
        template_c = self.env.get_template('entity.c.j2')
        
        entities = service_data.get('entities', {})
        if not entities:
            return []
        
        generated_files = []
        
        # 按服务前缀分组实体
        entity_groups: Dict[str, List[Dict]] = {}
        for entity_key, entity_def in entities.items():
            # 提取前缀（如 ApiPublish_UserInfo → ApiPublish）
            if '_' in entity_key:
                prefix = entity_key.split('_')[0]
            else:
                prefix = "common"
            
            if prefix not in entity_groups:
                entity_groups[prefix] = []
            
            class_name = self._normalize_type(entity_key)
            fields = entity_def.get('fields', [])
            
            # 检查是否有列表类型
            has_list = any(f.get('containerType') == 'list' for f in fields)
            
            entity_groups[prefix].append({
                'class_name': class_name,
                'description': entity_def.get('desc', f'{class_name} entity'),
                'fields': fields,
                'has_list': has_list,
            })
        
        # 为每个前缀生成一个头文件和源文件
        for prefix, entity_list in entity_groups.items():
            has_list = any(e['has_list'] for e in entity_list)
            file_name = f"{self.prefix}_{prefix.lower()}_entities"
            header_guard = self._to_header_guard(f"{prefix}_ENTITIES")
            
            # 渲染头文件
            content_h = template_h.render(
                header_guard=header_guard,
                file_name=file_name,
                prefix=self.prefix,
                description=f"{prefix} service entities",
                has_list=has_list,
                entities=entity_list,
            )
            
            output_file_h = self.output_dir / f'{file_name}.h'
            output_file_h.write_text(content_h, encoding='utf-8')
            generated_files.append(output_file_h)
            
            # 渲染源文件
            content_c = template_c.render(
                file_name=file_name,
                prefix=self.prefix,
                has_list=has_list,
                entities=entity_list,
            )
            
            output_file_c = self.output_dir / f'{file_name}.c'
            output_file_c.write_text(content_c, encoding='utf-8')
            generated_files.append(output_file_c)
        
        return generated_files

    def collect_entities(self, services: List[Dict[str, Any]]) -> Dict[str, Dict[str, Any]]:
        """从所有服务中收集实体定义，按前缀分组合并。"""
        entity_groups: Dict[str, Dict[str, Any]] = {}  # {prefix: {entity_key: entity_def}}
        
        for service in services:
            entities = service.get('entities', {})
            for entity_key, entity_def in entities.items():
                # 提取前缀
                if '_' in entity_key:
                    prefix = entity_key.split('_')[0]
                else:
                    prefix = "common"
                
                if prefix not in entity_groups:
                    entity_groups[prefix] = {}
                
                # 去重：如果实体已存在，跳过
                if entity_key not in entity_groups[prefix]:
                    entity_groups[prefix][entity_key] = entity_def
        
        return entity_groups

    def generate_merged_entities(self, entity_groups: Dict[str, Dict[str, Any]]) -> List[Path]:
        """根据合并后的实体数据生成头文件和源文件。"""
        template_h = self.env.get_template('entity.h.j2')
        template_c = self.env.get_template('entity.c.j2')
        
        generated_files = []
        
        for prefix, entities_dict in entity_groups.items():
            entity_list = []
            has_list = False
            
            for entity_key, entity_def in entities_dict.items():
                class_name = self._normalize_type(entity_key)
                fields = entity_def.get('fields', [])
                
                # 检查是否有列表类型
                if any(f.get('containerType') == 'list' for f in fields):
                    has_list = True
                
                entity_list.append({
                    'class_name': class_name,
                    'description': entity_def.get('desc', f'{class_name} entity'),
                    'fields': fields,
                    'has_list': any(f.get('containerType') == 'list' for f in fields),
                })
            
            file_name = f"{self.prefix}_{prefix.lower()}_entities"
            header_guard = self._to_header_guard(f"{prefix}_ENTITIES")
            
            # 渲染头文件
            content_h = template_h.render(
                header_guard=header_guard,
                file_name=file_name,
                prefix=self.prefix,
                description=f"{prefix} service entities",
                has_list=has_list,
                entities=entity_list,
            )
            
            output_file_h = self.output_dir / f'{file_name}.h'
            output_file_h.write_text(content_h, encoding='utf-8')
            generated_files.append(output_file_h)
            
            # 渲染源文件
            content_c = template_c.render(
                file_name=file_name,
                prefix=self.prefix,
                has_list=has_list,
                entities=entity_list,
            )
            
            output_file_c = self.output_dir / f'{file_name}.c'
            output_file_c.write_text(content_c, encoding='utf-8')
            generated_files.append(output_file_c)
        
        return generated_files

    def collect_error_codes(self, services: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """从所有服务中收集错误码，合并为一份数据，并检查冲突。"""
        merged_codes: List[Dict[str, Any]] = []
        code_index: Dict[int, Dict] = {}
        name_index: Dict[str, Dict] = {}
        fatal_conflicts: List[str] = []
        
        for service in services:
            api_group = service.get('apiGroup', {})
            code_define = api_group.get('codeDefine')
            if not code_define:
                continue
            service_name = api_group.get('name', code_define)
            min_code = api_group.get('minCode', 0)
            max_code = api_group.get('maxCode', 9999)
            
            error_codes = service.get('errorCodes', [])
            for code_group in error_codes:
                for code in code_group.get('codes', []):
                    code_val = code.get('code')
                    code_name = code.get('name')
                    code_desc = code.get('desc', '')
                    source = f"{service_name}({code_define})"
                    
                    if code_val in code_index:
                        existing = code_index[code_val]
                        if existing['desc'] == code_desc and existing['name'] == code_name:
                            continue
                        else:
                            fatal_conflicts.append(
                                f"[严重] 错误码数值冲突: code={code_val}\n"
                                f"       {source}: name={code_name}, desc={code_desc}\n"
                                f"       {existing['source']}: name={existing['name']}, desc={existing['desc']}"
                            )
                    else:
                        code_index[code_val] = {'desc': code_desc, 'name': code_name, 'source': source}
                    
                    if code_name in name_index:
                        existing_n = name_index[code_name]
                        if existing_n['code'] != code_val:
                            fatal_conflicts.append(
                                f"[严重] 错误码常量名冲突: name={code_name}\n"
                                f"       {source}: code={code_val}\n"
                                f"       {existing_n['source']}: code={existing_n['code']}"
                            )
                    else:
                        name_index[code_name] = {'code': code_val, 'source': source}
                    
                    merged_codes.append({
                        **code,
                        '_source': source,
                        '_min_code': min_code,
                        '_max_code': max_code,
                        '_service_name': service_name,
                    })
        
        if not merged_codes:
            return None
        
        return {
            'codes': merged_codes,
            'fatal_conflicts': fatal_conflicts,
        }
    
    def generate_merged_error_codes(self, merged_data: Dict[str, Any]) -> Optional[Path]:
        """将合并后的错误码数据写入 C 文件。"""
        template_h = self.env.get_template('error_code.h.j2')
        template_c = self.env.get_template('error_code.c.j2')
        
        fatal_conflicts = merged_data.get('fatal_conflicts', [])
        if fatal_conflicts:
            print("\n[错误] 检测到严重错误码冲突，已终止生成！请先修复以下问题：")
            for c in fatal_conflicts:
                print(f"  ✗ {c}")
            print()
            return None
        
        # 按服务分组
        groups: Dict[str, List] = {}
        for code in merged_data['codes']:
            key = f"{code['_service_name']}  [{code['_min_code']}-{code['_max_code']}]"
            groups.setdefault(key, []).append(code)
        
        file_name = f"{self.prefix}_return_code"
        header_guard = self._to_header_guard("RETURN_CODE")
        
        min_code = min(c.get('_min_code', 0) for c in merged_data['codes'])
        max_code = max(c.get('_max_code', 0) for c in merged_data['codes'])
        
        # 渲染头文件
        content_h = template_h.render(
            header_guard=header_guard,
            file_name=file_name,
            prefix=self.prefix,
            min_code=min_code,
            max_code=max_code,
            groups=groups,
        )
        
        output_file_h = self.output_dir / f'{file_name}.h'
        output_file_h.write_text(content_h, encoding='utf-8')
        
        # 渲染源文件
        content_c = template_c.render(
            file_name=file_name,
            prefix=self.prefix,
            groups=groups,
        )
        
        output_file_c = self.output_dir / f'{file_name}.c'
        output_file_c.write_text(content_c, encoding='utf-8')
        
        return output_file_h
    
    def generate_api_methods(self, service_data: Dict[str, Any]) -> List[Path]:
        """生成 API 方法头文件和源文件（每个方法一个文件对）"""
        template_h = self.env.get_template('api_method.h.j2')
        template_c = self.env.get_template('api_method.c.j2')
        
        api_group = service_data.get('apiGroup', {})
        service_name = api_group.get('name', 'UnknownService')
        interface_name = service_data.get('interfaceName', '')
        
        # 收集需要导入的实体头文件
        includes = set()
        entities = service_data.get('entities', {})
        for entity_key in entities.keys():
            if '_' in entity_key:
                prefix = entity_key.split('_')[0]
                includes.add(f"{self.prefix}_{prefix.lower()}_entities.h")
        
        methods = service_data.get('methods', [])
        generated_files = []
        
        for method in methods:
            method_name = method.get('name', 'unknownMethod')
            method_class_part = method_name[0].upper() + method_name[1:] if method_name else method_name
            class_name = f"{service_name}_{method_class_part}"
            
            # 收集该方法特有的导入
            method_includes = set(includes)
            
            # 处理返回类型
            return_type = method.get('returnType', '')
            if return_type and '_' in return_type:
                prefix = return_type.split('_')[0]
                method_includes.add(f"{self.prefix}_{prefix.lower()}_entities.h")
            
            # 过滤掉 StreamObserver 类型的参数
            normal_params = [p for p in method.get('parameters', []) 
                           if p.get('containerType') != 'StreamObserver']
            
            # 处理参数类型
            for param in normal_params:
                param_type = param.get('type', '')
                if '_' in param_type:
                    prefix = param_type.split('_')[0]
                    method_includes.add(f"{self.prefix}_{prefix.lower()}_entities.h")
            
            # 判断是否为 POST
            is_post = len(normal_params) > 0
            
            # 更新 method 数据
            method_copy = dict(method)
            method_copy['parameters'] = normal_params
            
            file_name = self.to_snake_case(class_name)
            header_guard = self._to_header_guard(class_name.upper())
            
            # 渲染头文件
            content_h = template_h.render(
                header_guard=header_guard,
                file_name=file_name,
                prefix=self.prefix,
                class_name=class_name,
                service_name=service_name,
                interface_name=interface_name,
                method=method_copy,
                includes=sorted(method_includes),
                is_post=is_post,
            )
            
            output_file_h = self.output_dir / f'{file_name}.h'
            output_file_h.write_text(content_h, encoding='utf-8')
            generated_files.append(output_file_h)
            
            # 渲染源文件
            content_c = template_c.render(
                file_name=file_name,
                prefix=self.prefix,
                class_name=class_name,
                service_name=service_name,
                interface_name=interface_name,
                method=method_copy,
                is_post=is_post,
            )
            
            output_file_c = self.output_dir / f'{file_name}.c'
            output_file_c.write_text(content_c, encoding='utf-8')
            generated_files.append(output_file_c)
        
        return generated_files
    
    def generate(self, json_file: str) -> Dict[str, Any]:
        """从 JSON 文件生成所有 C 代码"""
        # 读取 JSON 数据
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # 处理两种可能的结构
        services = data.get('services', [])
        
        if data.get('source') == 'nacos' and services and isinstance(services, list):
            if len(services) > 0 and 'metadata' in services[0] and 'services' in services[0].get('metadata', {}):
                all_services = []
                for svc_wrapper in services:
                    metadata = svc_wrapper.get('metadata', {})
                    if 'services' in metadata:
                        all_services.extend(metadata['services'])
                services = all_services
                print(f"从嵌套格式提取了 {len(services)} 个服务定义")
        
        results = {
            'entities': [],
            'error_codes': [],
            'api_methods': [],
        }
        
        # 收集并合并所有服务错误码
        merged_ec = self.collect_error_codes(services)
        if merged_ec is not None:
            ec_file = self.generate_merged_error_codes(merged_ec)
            if ec_file:
                results['error_codes'].append(ec_file)
        
        # 收集并合并所有服务实体
        merged_entities = self.collect_entities(services)
        if merged_entities:
            entity_files = self.generate_merged_entities(merged_entities)
            results['entities'].extend(entity_files)

        for service in services:
            # 生成 API 方法类
            api_method_files = self.generate_api_methods(service)
            results['api_methods'].extend(api_method_files)
        
        return results


def main():
    """命令行入口"""
    parser = argparse.ArgumentParser(description='C 代码生成工具 - 支持 JSON 文件或 Nacos 元数据')
    
    # 输入源（互斥）
    input_group = parser.add_mutually_exclusive_group(required=True)
    input_group.add_argument('json_file', nargs='?', help='JSON 元数据文件路径')
    input_group.add_argument('--nacos', metavar='URL', help='Nacos 地址（如 http://localhost:8848）')
    
    # Nacos 相关参数
    parser.add_argument('--all', action='store_true', help='获取所有服务的元数据（与 --nacos 配合使用）')
    
    # 通用参数
    parser.add_argument('-o', '--output', default='./src', help='输出目录（默认: ./src）')
    parser.add_argument('-t', '--template', default=None, help='模板目录（默认与脚本同目录的 template 文件夹）')
    parser.add_argument('-p', '--prefix', default='apg', help='C 代码前缀（默认: apg）')
    
    args = parser.parse_args()
    
    # 确定模板目录
    if args.template is None:
        script_dir = Path(__file__).parent
        template_dir = script_dir / 'template'
    else:
        template_dir = Path(args.template)
    
    # 创建生成器
    generator = CCodeGenerator(str(template_dir), args.output, args.prefix)
    
    # 处理输入源
    if args.nacos:
        # 从 Nacos 获取元数据
        fetcher = NacosMetadataFetcher(args.nacos)
        
        if args.all:
            # 获取所有服务
            print(f"从 Nacos 获取所有服务元数据: {args.nacos}")
            services = fetcher.fetch_all_services()
            if not services:
                print("未找到任何服务的元数据")
                return
            
            # 合并所有服务的元数据
            all_services_data = {'services': []}
            for svc in services:
                metadata = svc.get('metadata', {})
                if 'services' in metadata:
                    all_services_data['services'].extend(metadata['services'])
            
            # 保存到共享元数据目录（codegen 目录）
            script_dir = Path(__file__).parent
            temp_json = script_dir / 'nacos_metadata.json'
            temp_json.parent.mkdir(parents=True, exist_ok=True)
            with open(temp_json, 'w', encoding='utf-8') as f:
                json.dump(all_services_data, f, ensure_ascii=False, indent=2)
            
            print(f"元数据已保存到: {temp_json}")
            results = generator.generate(str(temp_json))
        else:
            print("错误: 使用 --nacos 时必须指定 --all")
            return
    else:
        # 从 JSON 文件生成
        results = generator.generate(args.json_file)
    
    # 输出结果
    print(f"\n{'='*60}")
    print(f"C 代码生成完成！")
    print(f"输出目录: {args.output}")
    print(f"{'='*60}")
    print(f"\n生成的实体类文件 ({len(results['entities'])})：")
    for f in results['entities']:
        print(f"  - {f}")
    print(f"\n生成的错误码文件 ({len(results['error_codes'])})：")
    for f in results['error_codes']:
        print(f"  - {f}")
    print(f"\n生成的 API 方法文件 ({len(results['api_methods'])})：")
    for f in results['api_methods']:
        print(f"  - {f}")


if __name__ == '__main__':
    main()
