
# -*- coding: utf-8 -*-
import sys

def is_valid_char(c):
    """检查字符是否适合作为 Kotlin 字符字面量"""
    if len(c) != 1:
        return False
    
    try:
        # 测试字符是否是常见字符（不是代理对）
        # 0x0000-0xFFFF 是 BMP 字符，适合单字符字面量
        code = ord(c)
        if code <= 0xFFFF:
            return True
        return False
    except:
        return False

def parse_tscharacters(file_path):
    mappings = {}
    
    # 添加常用的缺失字符映射（手动补充）
    additional_mappings = {
        '妳': '你',
        '著': '着',
        '裏': '里',
        '裡': '里',
        '爲': '为',
        '麼': '么',
        '麽': '么',
        '衆': '众',
        '羣': '群',
        '裏': '里',
        '牀': '床',
        '皁': '皂',
        '祕': '秘',
        '糉': '粽',
        '箇': '个',
        '纔': '才',
        '鉢': '钵',
        '隻': '只',
        '隣': '邻',
        '雞': '鸡',
        '電': '电',
        '雲': '云',
        '風': '风',
        '裏': '里'
    }
    
    # 先添加手动补充的映射
    for t, s in additional_mappings.items():
        if is_valid_char(t) and is_valid_char(s):
            mappings[t] = s
    
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            
            parts = line.split('\t')
            if len(parts) >= 2:
                traditional = parts[0]
                simplified_part = parts[1].split()[0]  # 取第一个简化选项
                
                # 只添加有效的单字符映射（不覆盖已有的手动补充）
                if (is_valid_char(traditional) and 
                    is_valid_char(simplified_part) and
                    traditional not in mappings):
                    mappings[traditional] = simplified_part
    
    return mappings

def generate_kotlin(mappings, output_file):
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('package com.example.LyricBox.utils\n\n')
        f.write('object ChineseConverter {\n\n')
        f.write('    private val traditionalToSimplifiedMap by lazy {\n')
        f.write('        buildMap {\n')
        
        count = 0
        for traditional, simplified in sorted(mappings.items()):
            # 确保字符可以正确作为字面量
            try:
                # 对于引号和反斜杠等特殊字符进行转义
                t_escaped = traditional
                s_escaped = simplified
                if t_escaped == "'":
                    t_escaped = "\\'"
                if t_escaped == "\\":
                    t_escaped = "\\\\"
                if s_escaped == "'":
                    s_escaped = "\\'"
                if s_escaped == "\\":
                    s_escaped = "\\\\"
                
                f.write(f"            put('{t_escaped}', '{s_escaped}')\n")
                count += 1
            except Exception as e:
                # 如果有问题就跳过
                continue
        
        f.write('        }\n')
        f.write('    }\n\n')
        f.write('    fun toSimplified(text: String): String {\n')
        f.write('        return text.map { char ->\n')
        f.write('            traditionalToSimplifiedMap[char] ?: char\n')
        f.write('        }.joinToString("")\n')
        f.write('    }\n')
        f.write('}\n')
        print(f'成功写入 {count} 个字符映射')

if __name__ == '__main__':
    input_file = r'D:\Users\B\Download\OpenCC-master\OpenCC-master\data\dictionary\TSCharacters.txt'
    output_file = r'd:\Users\B\AndroidStudioProjects\lyricEdite\app\src\main\java\com\example\LyricBox\utils\ChineseConverter.kt'
    
    print(f'解析 {input_file}...')
    mappings = parse_tscharacters(input_file)
    print(f'共找到 {len(mappings)} 个有效字符映射')
    
    print(f'生成 {output_file}...')
    generate_kotlin(mappings, output_file)
    print('完成！')
