path = r'E:\code\network-intent-agent-system\mac-tav-intent-agent\src\main\resources\application.yml'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove spring.ai.model.chat line (not needed / can cause issues)
content = content.replace('''    model:
      chat: dashscope
''', '')

# 2. Add enabled: true under a2a
content = content.replace('''      a2a:
        nacos:''', '''      a2a:
        enabled: true
        nacos:''')

# 3. Add enabled: true under server
content = content.replace('''        server:
          version:''', '''        server:
          enabled: true
          version:''')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print('application.yml updated')
