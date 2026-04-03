#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Java 代码生成工具 - 基于 Jinja2 模板引擎
从 JSON 元数据或 Nacos 生成 Java 实体类、API 接口和错误码定义
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
                        # 格式: api.md5.{interface}:{version}
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
        # 构建 dataId
        if api_md5:
            version_with_md5 = f"{version}-{api_md5}"
        else:
            version_with_md5 = version
        data_id = f"{interface}:{version_with_md5}:{group}:provider:{application}"
        
        # 获取配置
        config = self.get_config(data_id)
        if not config:
            return None
        
        # 提取 api.metadata
        parameters = config.get('parameters', {})
        compressed_data = parameters.get('api.metadata', '')
        
        if not compressed_data:
            # 尝试旧版本字段名
            compressed_data = parameters.get('api.publish.service.metadata', '')
        
        if not compressed_data:
            print(f"未找到 api.metadata: {interface}")
            return None
        
        # 解码元数据
        return self.decode_metadata(compressed_data)
    
    def fetch_all_services(self) -> List[Dict[str, Any]]:
        """获取所有带有 API 元数据的服务"""
        all_metadata = []
        
        # 获取所有服务
        public_services = self.list_services("PUBLIC-GROUP")
        internal_services = self.list_services("INTERNAL-GROUP")
        all_services = public_services + internal_services
        
        print(f"发现 {len(all_services)} 个 Nacos 服务")
        
        # 收集 api.md5
        app_md5_map = {}
        for svc in all_services:
            if svc['name'].startswith('providers:'):
                continue
            md5_map = self.extract_api_md5(svc['name'], svc['group'])
            if md5_map:
                app_md5_map[svc['name']] = md5_map
        
        print(f"发现 {len(app_md5_map)} 个应用包含 api.md5")
        
        # 获取 Dubbo 服务元数据
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
                
                # 获取 api.md5
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
                
                # 获取元数据
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


class JavaCodeGenerator:
    """Java 代码生成器"""
    
    # 类型映射：JSON 类型 -> Java 类型
    TYPE_MAPPING = {
        'string': 'String',
        'int': 'int',
        'long': 'long',
        'bool': 'boolean',
        'float': 'float',
        'double': 'double',
        'byte': 'byte',
        'short': 'short',
        'char': 'char',
    }
    
    # 包装类映射（用于泛型参数）
    WRAPPER_MAPPING = {
        'int': 'Integer',
        'long': 'Long',
        'bool': 'Boolean',
        'boolean': 'Boolean',
        'float': 'Float',
        'double': 'Double',
        'byte': 'Byte',
        'short': 'Short',
        'char': 'Character',
    }
    
    # 客户端 SDK 基础包名
    CLIENT_SDK_PACKAGE = 'com.pocrd.clientsdk'
    # 自动生成代码包名
    AUTOGEN_PACKAGE = 'com.pocrd.clientsdk.autogen'
    
    def __init__(self, template_dir: str, output_dir: str):
        """
        初始化代码生成器
        
        Args:
            template_dir: Jinja2 模板目录
            output_dir: 代码输出目录
        """
        self.template_dir = Path(template_dir)
        self.output_dir = Path(output_dir)
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
        self.env.filters['to_java_type'] = self.to_java_type
        self.env.filters['to_wrapper_type'] = self.to_wrapper_type
        self.env.filters['to_class_name'] = self.to_class_name
        self.env.filters['to_package_name'] = self.to_package_name
        self.env.filters['to_camel_case'] = self.to_camel_case
        self.env.filters['to_pascal_case'] = self.to_pascal_case
        self.env.filters['escape_java_string'] = self.escape_java_string
        # 实体 JSON 序列化相关过滤器
        self.env.filters['to_json_value'] = self.to_json_value
        self.env.filters['default_value'] = self.default_value
        self.env.filters['parse_json_value'] = self.parse_json_value
        # 注册模板测试：判断类型是否为实体类（含 '_'，非内置基础类型）
        self.env.tests['entity_type'] = lambda t: '_' in t and t not in self.TYPE_MAPPING
        # 注册模板测试：判断类型是否为基础类型（非对象类型）
        self.env.tests['primitive_type'] = lambda t: t in self.TYPE_MAPPING
    
    def _normalize_type(self, type_str: str) -> str:
        """将类型字符串规范化：
        - 内置基础类型 → 映射表查找
        - 全限定名（含 '.'） → 只取最后一段（去包名）
        - ServiceId_ClassName 格式（含 '_'） → 保留原样
        - 如果同时含 '.' 和 '_'，说明元数据格式异常，报错
        """
        if not type_str:
            return type_str
        # 内置基础类型
        if type_str in self.TYPE_MAPPING:
            return self.TYPE_MAPPING[type_str]
        # 处理错误生成的类型名（如 ApiPublish_String 应该映射为 String）
        if '_' in type_str:
            parts = type_str.split('_')
            if len(parts) == 2:
                potential_builtin = parts[1].lower()
                if potential_builtin in self.TYPE_MAPPING:
                    return self.TYPE_MAPPING[potential_builtin]
        has_dot = '.' in type_str
        has_underscore = '_' in type_str
        if has_dot and has_underscore:
            raise ValueError(
                f"元数据类型格式异常：'{type_str}' 同时包含 '.' 和 '_'，"
                f"期望格式为全限定名（如 com.example.Foo）或 ServiceId_ClassName"
            )
        if has_dot:
            # 全限定名 → 去包名
            return type_str.split('.')[-1]
        # ServiceId_ClassName 或普通类名 → 保留原样
        return type_str

    def to_java_type(self, type_str: str, container_type: Optional[str] = None) -> str:
        """
        将 JSON 类型转换为 Java 类型
        
        Args:
            type_str: JSON 中的类型字符串
            container_type: 容器类型（list, set, array 等）
        
        Returns:
            Java 类型字符串
        """
        if not type_str:
            return 'void'

        # 处理数组类型（以 ; 结尾表示数组）
        if type_str.endswith(';'):
            base_type = type_str[:-1]
            java_base = self._normalize_type(base_type)
            return f'{java_base}[]'
        
        # 获取基础 Java 类型（规范化，去掉前缀）
        java_type = self._normalize_type(type_str)
        
        # 处理容器类型（作为 container_type 参数传入）
        if container_type:
            container = container_type.lower()
            if container == 'list':
                wrapper_type = self.WRAPPER_MAPPING.get(java_type, java_type)
                return f'List<{wrapper_type}>'
            elif container == 'set':
                wrapper_type = self.WRAPPER_MAPPING.get(java_type, java_type)
                return f'Set<{wrapper_type}>'
            elif container == 'array':
                return f'{java_type}[]'
        
        # 处理容器类型本身作为 type_str 的情况（如返回类型是 list/set）
        type_lower = type_str.lower()
        if type_lower == 'list':
            return 'List'
        elif type_lower == 'set':
            return 'Set'
        
        # 处理 stream 类型（流式响应）
        if type_lower == 'stream':
            return 'Stream'
        
        # 处理 boolean 基本类型 - 在泛型中需要使用 Boolean
        if type_lower == 'boolean':
            return 'boolean'
        
        return java_type
    
    def to_wrapper_type(self, type_str: str) -> str:
        """将基本类型转换为包装类，非基础类型原样返回"""
        # to_wrapper_type 接收的已经是 java_type（经过 to_java_type 转换后）
        # 容器类型直接返回，不需要转换
        if type_str in ('List', 'Set'):
            return type_str
        # void 类型特殊处理 - API方法返回void时，使用Void.class
        if type_str == 'void':
            return 'Void'
        # 流类型特殊处理
        if type_str in ('stream', 'Stream'):
            return 'Object'  # 流类型使用Object作为泛型参数
        return self.WRAPPER_MAPPING.get(type_str, type_str)
    
    def to_class_name(self, full_type: str) -> str:
        """
        从元数据类型名获取 Java 类名：
        - 全限定名（含 '.'）→ 去包名
        - ServiceId_ClassName 格式（含 '_'）→ 保留原样（前缀是服务标识，隐式隔离）
        """
        return self._normalize_type(full_type)
    
    def to_package_name(self, full_type: str) -> str:
        """
        从完整类型名中提取包名
        
        Args:
            full_type: 如 "com.example.AddressInfo"
        
        Returns:
            包名，如 "com.example"
        """
        if '.' in full_type:
            return full_type.rsplit('.', 1)[0]
        return ''
    
    def to_camel_case(self, snake_str: str) -> str:
        """将蛇形命名转换为驼峰命名（首字母小写）"""
        components = snake_str.split('_')
        return components[0] + ''.join(x.capitalize() for x in components[1:])
    
    def to_pascal_case(self, snake_str: str) -> str:
        """将蛇形命名转换为帕斯卡命名（首字母大写）"""
        components = snake_str.split('_')
        return ''.join(x.capitalize() for x in components)
    
    def escape_java_string(self, s: str) -> str:
        """转义 Java 字符串中的特殊字符"""
        if not s:
            return ''
        return s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t')

    def to_json_value(self, field_name: str, type_str: str = '', container_type: str = '') -> str:
        """生成字段序列化为 JSON 的代码片段"""
        # 如果有容器类型，使用 JsonUtil 处理集合
        if container_type:
            return f'com.pocrd.clientsdk.JsonUtil.toJson({field_name})'
        # 实体类型（含 _）调用自身的 toJson() 方法
        if '_' in type_str:
            return f'{field_name}.toJson()'
        # 基础类型使用 JsonUtil
        return f'com.pocrd.clientsdk.JsonUtil.toJson({field_name})'

    def default_value(self, type_str: str, container_type: Optional[str] = None) -> str:
        """返回类型的默认值（用于 fromJson 初始化）"""
        # 容器类型默认返回空集合，避免空指针异常
        if container_type:
            container = container_type.lower()
            if container == 'list':
                return 'java.util.Collections.emptyList()'
            if container == 'set':
                return 'java.util.Collections.emptySet()'
        
        defaults = {
            'string': '""',
            'int': '0',
            'long': '0L',
            'bool': 'false',
            'float': '0.0f',
            'double': '0.0',
            'byte': '0',
            'short': '0',
            'char': "'\\0'",
        }
        return defaults.get(type_str, 'null')

    def parse_json_value(self, type_str: str, container_type: Optional[str] = None) -> str:
        """生成从 JSON 字符串解析字段值的代码片段，支持容器类型"""
        # 单值基础类型解析（使用 value 变量）
        single_parsers = {
            'string': 'value.replace("\\"", "")',
            'int': 'Integer.parseInt(value)',
            'long': 'Long.parseLong(value)',
            'bool': 'Boolean.parseBoolean(value)',
            'float': 'Float.parseFloat(value)',
            'double': 'Double.parseDouble(value)',
            'byte': 'Byte.parseByte(value)',
            'short': 'Short.parseShort(value)',
        }
        
        # 列表元素基础类型解析（使用 v 变量）
        element_parsers = {
            'string': 'v.replace("\\"", "")',
            'int': 'Integer.parseInt(v)',
            'long': 'Long.parseLong(v)',
            'bool': 'Boolean.parseBoolean(v)',
            'float': 'Float.parseFloat(v)',
            'double': 'Double.parseDouble(v)',
            'byte': 'Byte.parseByte(v)',
            'short': 'Short.parseShort(v)',
        }

        # 如果有容器类型，生成容器解析代码
        if container_type:
            container = container_type.lower()
            if container in ('list', 'array'):
                element_type = type_str
                # 实体类型（含 _）使用 fromJson 解析
                if '_' in element_type:
                    class_name = self.to_class_name(element_type)
                    element_parser = f'{class_name}.fromJson(v)'
                else:
                    element_parser = element_parsers.get(element_type, 'v.replace("\\"", "")')
                return f'com.pocrd.clientsdk.JsonUtil.parseList(value, v -> {element_parser})'
            elif container == 'set':
                element_type = type_str
                # 实体类型（含 _）使用 fromJson 解析
                if '_' in element_type:
                    class_name = self.to_class_name(element_type)
                    element_parser = f'{class_name}.fromJson(v)'
                else:
                    element_parser = element_parsers.get(element_type, 'v.replace("\\"", "")')
                return f'new java.util.HashSet<>(com.pocrd.clientsdk.JsonUtil.parseList(value, v -> {element_parser}))'

        # 实体类型（含 _）
        if '_' in type_str:
            class_name = self.to_class_name(type_str)
            return f'{class_name}.fromJson(value)'

        return single_parsers.get(type_str, 'null /* unsupported type */')
    
    def generate_entities(self, service_data: Dict[str, Any], package_prefix: str = None) -> List[Path]:
        """
        生成实体类
        
        Args:
            service_data: 服务数据
            package_prefix: 实体类包名前缀（默认使用客户端SDK包名）
        
        Returns:
            生成的文件路径列表
        """
        if package_prefix is None:
            package_prefix = f'{self.AUTOGEN_PACKAGE}.entity'
        
        template = self.env.get_template('entity.java.j2')
        entities = service_data.get('entities', {})
        generated_files = []
        
        for entity_key, entity_def in entities.items():
            class_name = self.to_class_name(entity_key)
            fields = entity_def.get('fields', [])

            # 检查字段中是否用到 List/Set
            has_list = any(
                self.to_java_type(f.get('type', ''), f.get('containerType')).startswith('List<')
                for f in fields
            )
            has_set = any(
                self.to_java_type(f.get('type', ''), f.get('containerType')).startswith('Set<')
                for f in fields
            )
            # 检查是否包含嵌套实体类（类型含 '_'）
            has_nested_entity = any(
                '_' in f.get('type', '')
                for f in fields
            )

            # 构建实体数据
            entity_data = {
                'package': package_prefix,
                'class_name': class_name,
                'description': entity_def.get('desc', f'{class_name} 实体'),
                'is_record': entity_def.get('isRecord', True),
                'fields': fields,
                'original_type': entity_key,
                'has_list': has_list,
                'has_set': has_set,
                'has_nested_entity': has_nested_entity,
            }
            
            # 渲染模板
            content = template.render(**entity_data)
            
            # 写入文件（使用包名路径）
            package_path = package_prefix.replace('.', '/')
            output_file = self.output_dir / package_path / f'{class_name}.java'
            output_file.parent.mkdir(parents=True, exist_ok=True)
            output_file.write_text(content, encoding='utf-8')
            generated_files.append(output_file)
        
        return generated_files
    
    def collect_error_codes(self, services: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """
        从所有服务中收集错误码，合并为一份数据，并检查冲突。

        规则：
          - code值+name+desc 完全相同 → 去重，视为同一条目
          - code值相同但 desc 不同    → 严重错误，终止生成
          - name 相同但 code值不同    → 严重错误，终止生成

        Returns:
            合并后的错误码数据 dict，包含 codes / fatal_conflicts 字段；
            无任何错误码时返回 None
        """
        merged_codes: List[Dict[str, Any]] = []   # 最终合并列表（已去重）
        # code值 -> {'desc': ..., 'name': ..., 'source': ...}
        code_index: Dict[int, Dict] = {}
        # 常量名 -> {'code': ..., 'source': ...}
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

                    # 检查 code 数值
                    if code_val in code_index:
                        existing = code_index[code_val]
                        if existing['desc'] == code_desc and existing['name'] == code_name:
                            # 完全相同 → 去重，跳过
                            continue
                        else:
                            # desc 或 name 不同 → 严重冲突
                            fatal_conflicts.append(
                                f"[严重] 错误码数值冲突: code={code_val}\n"
                                f"       {source}: name={code_name}, desc={code_desc}\n"
                                f"       {existing['source']}: name={existing['name']}, desc={existing['desc']}"
                            )
                            # 仍记录进 index，继续扫描其他冲突
                    else:
                        code_index[code_val] = {'desc': code_desc, 'name': code_name, 'source': source}

                    # 检查常量名
                    if code_name in name_index:
                        existing_n = name_index[code_name]
                        if existing_n['code'] != code_val:
                            # name 相同但 code 值不同 → 严重冲突
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

    def generate_merged_error_codes(self, merged_data: Dict[str, Any],
                                    class_name: str = 'ServiceReturnCode',
                                    package_prefix: str = None) -> Optional[Path]:
        """
        将合并后的错误码数据写入一个 Java 文件。
        有严重冲突时打印错误并返回 None（终止生成）。
        """
        if package_prefix is None:
            package_prefix = f'{self.AUTOGEN_PACKAGE}.constant'

        fatal_conflicts = merged_data.get('fatal_conflicts', [])
        if fatal_conflicts:
            print("\n[错误] 检测到严重错误码冲突，已终止生成！请先修复以下问题：")
            for c in fatal_conflicts:
                print(f"  ✗ {c}")
            print()
            return None

        # 按服务分组，便于模板中分段注释
        groups: Dict[str, List] = {}
        for code in merged_data['codes']:
            key = f"{code['_service_name']}  [{code['_min_code']}-{code['_max_code']}]"
            groups.setdefault(key, []).append(code)

        template = self.env.get_template('errorcode.java.j2')
        content = template.render(
            package=package_prefix,
            class_name=class_name,
            min_code=min(c.get('_min_code', 0) for c in merged_data['codes']),
            max_code=max(c.get('_max_code', 0) for c in merged_data['codes']),
            service_name='All Services',
            codes=merged_data['codes'],
            groups=groups,
            has_conflicts=False,
        )

        package_path = package_prefix.replace('.', '/')
        output_file = self.output_dir / package_path / f'{class_name}.java'
        output_file.parent.mkdir(parents=True, exist_ok=True)
        output_file.write_text(content, encoding='utf-8')
        return output_file

    def generate_api_methods(self, service_data: Dict[str, Any], package_prefix: str = None) -> List[Path]:
        """
        生成 API 方法类（每个方法一个类文件）
        
        Args:
            service_data: 服务数据
            package_prefix: API 包名前缀（默认使用客户端SDK包名）
        
        Returns:
            生成的文件路径列表
        """
        if package_prefix is None:
            package_prefix = self.AUTOGEN_PACKAGE
        
        template = self.env.get_template('apimethod.java.j2')
        api_group = service_data.get('apiGroup', {})
        service_name = api_group.get('name', 'UnknownService')
        interface_name = service_data.get('interfaceName', '')
        
        # 收集需要导入的实体类
        base_imports = set()
        entities = service_data.get('entities', {})
        for entity_key in entities.keys():
            class_name = self.to_class_name(entity_key)
            base_imports.add(f'{package_prefix}.entity.{class_name}')
        
        # 添加错误码导入（统一指向合并后的 ServiceReturnCode）
        code_define = api_group.get('codeDefine')
        if code_define:
            base_imports.add(f'{package_prefix}.constant.ServiceReturnCode')
        
        methods = service_data.get('methods', [])
        generated_files = []
        
        for method in methods:
            method_name = method.get('name', 'unknownMethod')
            # 类名格式：{ServiceName}_{MethodNameWithFirstLetterUppercased}
            # 方法名保持驼峰，仅首字母大写
            method_class_part = method_name[0].upper() + method_name[1:] if method_name else method_name
            class_name = f"{service_name}_{method_class_part}"
            
            # 收集该方法特有的导入
            imports = set(base_imports)
            
            # 处理返回类型
            return_type = method.get('returnType', '')
            normalized_return = self._normalize_type(return_type.replace(';', '')) if return_type else ''
            if return_type and normalized_return not in self.TYPE_MAPPING.values():
                if '_' in return_type:
                    entity_class = self.to_class_name(return_type.replace(';', ''))
                    imports.add(f'{package_prefix}.entity.{entity_class}')
            
            # 过滤掉 StreamObserver 类型的参数（这是服务端回调，不应暴露给客户端）
            stream_params = [p for p in method.get('parameters', []) 
                           if p.get('containerType') == 'StreamObserver']
            normal_params = [p for p in method.get('parameters', []) 
                           if p.get('containerType') != 'StreamObserver']
            
            # 处理参数类型（只处理非 StreamObserver 参数）
            for param in normal_params:
                param_type = param.get('type', '')
                normalized_type = self._normalize_type(param_type)
                if param_type and normalized_type not in self.TYPE_MAPPING.values():
                    if '_' in param_type:
                        entity_class = self.to_class_name(param_type)
                        imports.add(f'{package_prefix}.entity.{entity_class}')
            
            # 判断是否为流式接口
            is_stream = len(stream_params) > 0
            
            # 判断是否为 POST（有普通参数就用 POST）
            is_post = len(normal_params) > 0
            
            # 更新 method 数据，移除 StreamObserver 参数
            method = dict(method)
            method['parameters'] = normal_params

            # 构建方法数据
            method_data = {
                'package': f'{package_prefix}.api',
                'service_name': service_name,
                'interface_name': interface_name,
                'class_name': class_name,
                'method': method,
                'imports': sorted(imports),
                'code_define': code_define,
                'is_post': is_post,
                'is_stream': is_stream,
            }
            
            # 渲染模板
            content = template.render(**method_data)
            
            # 写入文件（使用包名路径）
            package_path = f'{package_prefix}.api'.replace('.', '/')
            output_file = self.output_dir / package_path / f'{class_name}.java'
            output_file.parent.mkdir(parents=True, exist_ok=True)
            output_file.write_text(content, encoding='utf-8')
            generated_files.append(output_file)
        
        return generated_files
    
    def check_base_classes(self) -> List[Path]:
        """
        检查基础 SDK 类是否存在
        基础类应放在 com/pocrd/clientsdk/ 目录下，作为静态文件管理
        
        Returns:
            基础类文件路径列表
        """
        base_package = self.CLIENT_SDK_PACKAGE
        package_path = base_package.replace('.', '/')
        
        # 基础类文件列表
        base_classes = ['ReturnCode.java', 'ApiMethod.java', 'JsonUtil.java']
        found_files = []
        
        for class_file in base_classes:
            base_file = self.output_dir / package_path / class_file
            if base_file.exists():
                found_files.append(base_file)
            else:
                print(f"警告: 基础类不存在: {base_file}")
        
        return found_files
    
    def generate(self, json_file: str, package_prefix: str = None) -> Dict[str, Any]:
        """
        从 JSON 文件生成所有 Java 代码
        
        Args:
            json_file: JSON 元数据文件路径
            package_prefix: 包名前缀（默认使用客户端SDK包名）
        
        Returns:
            生成结果统计
        """
        if package_prefix is None:
            package_prefix = self.AUTOGEN_PACKAGE

        # 生成前先清除 autogen 目录
        autogen_path = self.output_dir / package_prefix.replace('.', '/')
        if autogen_path.exists():
            import shutil
            shutil.rmtree(autogen_path)
            print(f"已清除旧文件：{autogen_path}")
        autogen_path.mkdir(parents=True, exist_ok=True)

        # 读取 JSON 数据
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # 处理两种可能的结构：
        # 1. 直接格式: { "services": [...] }
        # 2. decode_nacos_metadata.py 生成的嵌套格式: { "services": [ { "metadata": { "services": [...] } } ] }
        services = data.get('services', [])
        
        # 如果是嵌套格式（包含 source 字段且 services 项有 metadata 字段），提取实际的 API 服务定义
        if data.get('source') == 'nacos' and services and isinstance(services, list):
            if len(services) > 0 and 'metadata' in services[0] and 'services' in services[0].get('metadata', {}):
                # 这是嵌套格式，需要合并所有服务的 metadata.services
                all_services = []
                for svc_wrapper in services:
                    metadata = svc_wrapper.get('metadata', {})
                    if 'services' in metadata:
                        all_services.extend(metadata['services'])
                services = all_services
                print(f"从嵌套格式提取了 {len(services)} 个服务定义")
        results = {
            'base_classes': [],
            'entities': [],
            'error_codes': [],
            'api_methods': [],
        }
        
        # 检查基础 SDK 类是否存在（ReturnCode.java 和 ApiMethod.java）
        results['base_classes'] = self.check_base_classes()

        # 收集并合并所有服务错误码（包含冲突检测）
        merged_ec = self.collect_error_codes(services)
        if merged_ec is not None:
            ec_file = self.generate_merged_error_codes(
                merged_ec,
                class_name='ServiceReturnCode',
                package_prefix=f'{package_prefix}.constant',
            )
            if ec_file:
                results['error_codes'].append(ec_file)
        
        for service in services:
            # 生成实体类（每个实体一个文件）
            entity_files = self.generate_entities(service, f'{package_prefix}.entity')
            results['entities'].extend(entity_files)
            
            # 生成 API 方法类（每个方法一个文件）
            api_method_files = self.generate_api_methods(service, package_prefix)
            results['api_methods'].extend(api_method_files)
        
        return results


def main():
    """命令行入口"""
    parser = argparse.ArgumentParser(description='Java 代码生成工具 - 支持 JSON 文件或 Nacos 元数据')
    
    # 输入源（互斥）
    input_group = parser.add_mutually_exclusive_group(required=True)
    input_group.add_argument('json_file', nargs='?', help='JSON 元数据文件路径')
    input_group.add_argument('--nacos', metavar='URL', help='Nacos 地址（如 http://localhost:8848）')
    
    # Nacos 相关参数
    parser.add_argument('--service', help='服务接口名（与 --nacos 配合使用）')
    parser.add_argument('--version', default='1.0.0', help='服务版本（默认: 1.0.0）')
    parser.add_argument('--group', default='public', help='服务分组（默认: public）')
    parser.add_argument('--application', default='api-publish-service', help='应用名（默认: api-publish-service）')
    parser.add_argument('--all', action='store_true', help='获取所有服务的元数据（与 --nacos 配合使用）')
    
    # 通用参数
    parser.add_argument('-o', '--output', default='.', help='输出目录')
    parser.add_argument('-t', '--template', default=None, help='模板目录（默认与脚本同目录的 template 文件夹）')
    parser.add_argument('-p', '--package', default='com.pocrd.clientsdk.autogen', help='包名前缀（默认: com.pocrd.clientsdk.autogen）')
    
    args = parser.parse_args()
    
    # 确定模板目录
    if args.template is None:
        script_dir = Path(__file__).parent
        template_dir = script_dir / 'template'
    else:
        template_dir = Path(args.template)
    
    # 创建生成器
    generator = JavaCodeGenerator(str(template_dir), args.output)
    
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
            
            # 保存到临时 JSON 文件
            temp_json = Path(args.output) / 'nacos_metadata.json'
            temp_json.parent.mkdir(parents=True, exist_ok=True)
            with open(temp_json, 'w', encoding='utf-8') as f:
                json.dump(all_services_data, f, ensure_ascii=False, indent=2)
            
            print(f"元数据已保存到: {temp_json}")
            results = generator.generate(str(temp_json), args.package)
        else:
            # 获取单个服务
            if not args.service:
                print("错误: 使用 --nacos 时必须指定 --service 或使用 --all")
                return
            
            print(f"从 Nacos 获取服务元数据: {args.service}")
            metadata = fetcher.fetch_service_metadata(
                args.service, args.version, args.group, args.application
            )
            
            if not metadata:
                print(f"获取元数据失败: {args.service}")
                return
            
            # 保存到临时 JSON 文件
            temp_json = Path(args.output) / 'nacos_metadata.json'
            temp_json.parent.mkdir(parents=True, exist_ok=True)
            with open(temp_json, 'w', encoding='utf-8') as f:
                json.dump(metadata, f, ensure_ascii=False, indent=2)
            
            print(f"元数据已保存到: {temp_json}")
            results = generator.generate(str(temp_json), args.package)
    else:
        # 从 JSON 文件生成
        results = generator.generate(args.json_file, args.package)
    
    # 输出结果
    print(f"\n{'='*60}")
    print(f"代码生成完成！")
    print(f"输出目录: {args.output}")
    print(f"{'='*60}")
    print(f"\n基础 SDK 类 ({len(results['base_classes'])})：")
    for f in results['base_classes']:
        print(f"  - {f}")
    print(f"\n生成的实体类 ({len(results['entities'])})：")
    for f in results['entities']:
        print(f"  - {f}")
    print(f"\n生成的业务错误码类 ({len(results['error_codes'])})：")
    for f in results['error_codes']:
        print(f"  - {f}")
    print(f"\n生成的 API 方法类 ({len(results['api_methods'])})：")
    for f in results['api_methods']:
        print(f"  - {f}")


if __name__ == '__main__':
    main()
