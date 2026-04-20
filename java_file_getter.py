import os

def combine_java_files(input_folder, output_file):
    with open(output_file, 'w', encoding='utf-8') as outfile:
        
        for root, dirs, files in os.walk(input_folder):
            for file in files:
                if file.endswith(".java"):
                    file_path = os.path.join(root, file)
                    
                    try:
                        with open(file_path, 'r', encoding='utf-8') as infile:
                            outfile.write(f"\n{'='*80}\n")
                            outfile.write(f"FILE: {file_path}\n")
                            outfile.write(f"{'='*80}\n\n")
                            
                            outfile.write(infile.read())
                            outfile.write("\n\n")
                    
                    except Exception as e:
                        print(f"Error reading {file_path}: {e}")

    print(f"All Java files combined into: {output_file}")


# ===== USAGE =====
input_folder = r"D:\Projects\SocietyHive_Test5\app\src\main\java\com\example\societyhive_test5"
output_file = "combined_java_files.txt"

combine_java_files(input_folder, output_file)
