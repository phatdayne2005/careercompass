import urllib.request
import re
import os

url = 'https://roadmap.sh/frontend'
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req) as response:
        html = response.read().decode('utf-8')
        print('HTML length:', len(html))
        # Find window.__remixContext or script containing it
        match = re.search(r'window\.__remixContext\s*=\s*(.*?);\s*</script>', html, re.DOTALL)
        if match:
            print('Found __remixContext!')
            with open('remix_context.json', 'w', encoding='utf-8') as f:
                f.write(match.group(1))
        else:
            print('Not found window.__remixContext')
            # Let's search for any JSON script
            if '__remixContext' in html:
                print('__remixContext is present in html')
                # Let's print some lines containing it
                for line in html.split('\n'):
                    if '__remixContext' in line:
                        print('Line:', line[:200])
except Exception as e:
    print('Error:', e)
