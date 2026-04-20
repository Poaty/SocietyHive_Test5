import os

def combine_xml_files(input_folder, output_file):
    found = False

    with open(output_file, 'w', encoding='utf-8') as outfile:
        for root, dirs, files in os.walk(input_folder):
            for file in files:
                if file.lower().endswith(".xml"):
                    found = True
                    file_path = os.path.join(root, file)

                    try:
                        with open(file_path, 'r', encoding='utf-8') as infile:
                            outfile.write("\n" + "="*80 + "\n")
                            outfile.write(f"FILE: {file_path}\n")
                            outfile.write("="*80 + "\n\n")

                            outfile.write(infile.read())
                            outfile.write("\n\n")

                    except Exception as e:
                        print(f"Error reading {file_path}: {e}")

    if not found:
        print("❌ No XML files found.")
    else:
        print(f"✅ Done! XML files combined into: {output_file}")


# ===== USAGE =====
input_folder = r"D:\Projects\SocietyHive_Test5\app\src\main\res"
output_file = "combined_xml_files.txt"

combine_xml_files(input_folder, output_file)
