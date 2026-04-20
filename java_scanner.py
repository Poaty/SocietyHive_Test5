import os
import re

CLASS_PATTERN = re.compile(r'\bclass\s+([A-Za-z_][A-Za-z0-9_]*)')
METHOD_PATTERN = re.compile(
    r'(public|private|protected)?\s*(static\s+)?[A-Za-z0-9_<>\[\]]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*\('
)

GENERIC_NAMES = {
    "loadData", "getData", "setData", "handleClick", "processData",
    "helper", "utils", "manager", "doStuff", "attemptCreate",
    "applySearch", "loadSocieties", "text", "safeString"
}

def analyze_java_file(file_path):
    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()

    classes = CLASS_PATTERN.findall(content)
    methods = [match[2] for match in METHOD_PATTERN.findall(content)]
    comments = re.findall(r'//.*|/\*[\s\S]*?\*/', content)

    generic_hits = [name for name in methods if name in GENERIC_NAMES]

    return {
        "file": file_path,
        "classes": classes,
        "method_count": len(methods),
        "methods": methods,
        "comment_count": len(comments),
        "generic_method_names": generic_hits,
    }

def scan_folder(folder_path):
    results = []
    for root, _, files in os.walk(folder_path):
        for file in files:
            if file.lower().endswith(".java"):
                full_path = os.path.join(root, file)
                try:
                    results.append(analyze_java_file(full_path))
                except Exception as e:
                    results.append({
                        "file": full_path,
                        "error": str(e)
                    })
    return results

def write_report(results, output_file):
    with open(output_file, "w", encoding="utf-8") as f:
        for item in results:
            f.write("=" * 100 + "\n")
            f.write(f"FILE: {item['file']}\n")

            if "error" in item:
                f.write(f"ERROR: {item['error']}\n\n")
                continue

            f.write(f"CLASSES: {', '.join(item['classes']) if item['classes'] else 'None found'}\n")
            f.write(f"METHOD COUNT: {item['method_count']}\n")
            f.write(f"COMMENT COUNT: {item['comment_count']}\n")
            f.write("METHODS:\n")
            for method in item["methods"]:
                f.write(f"  - {method}\n")

            if item["generic_method_names"]:
                f.write("GENERIC-SOUNDING METHOD NAMES TO REVIEW:\n")
                for name in item["generic_method_names"]:
                    f.write(f"  - {name}\n")
            else:
                f.write("GENERIC-SOUNDING METHOD NAMES TO REVIEW: None\n")

            f.write("\n")

if __name__ == "__main__":
    input_folder = r"D:\Projects\SocietyHive_Test5\app\src\main\java\com\example\societyhive_test5"
    output_file = "java_style_review_report.txt"

    results = scan_folder(input_folder)
    write_report(results, output_file)

    print(f"Review report written to {output_file}")
