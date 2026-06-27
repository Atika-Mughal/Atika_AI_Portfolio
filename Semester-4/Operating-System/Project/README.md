# 🗂️ Empty File Finder

<p align="center">

![Bash](https://img.shields.io/badge/Bash_Script-121011?style=for-the-badge&logo=gnubash&logoColor=white)
![Linux](https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black)
![Operating System](https://img.shields.io/badge/Operating%20Systems-6A5ACD?style=for-the-badge)
![Shell Script](https://img.shields.io/badge/Shell_Scripting-C770F0?style=for-the-badge)

</p>

---

# 📌 Project Overview

**Empty File Finder** is a Bash scripting utility designed to scan directories and identify **empty files (0-byte files)** within a Linux file system. The project demonstrates practical Operating System concepts such as shell scripting, file handling, directory traversal, and command-line automation.

This project was developed as part of the **Operating Systems** course during my **BS Artificial Intelligence** program.

---

# 🎯 Objectives

- Identify empty files within a directory.
- Automate file system inspection using Bash scripting.
- Apply Linux commands for file management.
- Demonstrate practical Operating System concepts.
- Improve efficiency in organizing and maintaining file systems.

---

# ✨ Features

- 📂 Scan any directory
- 📁 Recursive directory traversal
- 📄 Detect 0-byte files
- ⚡ Lightweight and fast
- 🖥️ Command-line interface
- 📋 Display complete file paths
- 🐧 Compatible with Linux environments

---

# 🛠️ Technologies Used

| Technology | Purpose |
|------------|---------|
| Bash | Shell Scripting |
| Linux | Operating System |
| Shell Commands | File Handling |
| Terminal | Command Line Interface |

---

# 📂 Project Structure

```text
Empty-File-Finder/
│
├── empty_file_finder.sh
├── README.md
├── Screenshots/
├── Sample_Directory/
└── Documentation/
```

---

# ⚙️ How It Works

The script performs the following steps:

1. Accepts a directory path.
2. Traverses all folders and subfolders.
3. Checks every file.
4. Identifies files with a size of **0 bytes**.
5. Displays the path of each empty file.
6. Shows the total number of empty files found.

---

# 🔄 Workflow

```text
User Input
     │
     ▼
Enter Directory Path
     │
     ▼
Traverse Directories
     │
     ▼
Check File Size
     │
     ▼
Is File Empty?
     │
 ┌───┴────┐
 │        │
Yes      No
 │        │
 ▼        ▼
Display  Continue
Result   Scanning
```

---

# ▶️ How to Run

### Clone the repository

```bash
git clone https://github.com/Atika-Mughal/Empty-File-Finder.git
```

### Navigate to the project folder

```bash
cd Empty-File-Finder
```

### Give execute permission

```bash
chmod +x empty_file_finder.sh
```

### Run the script

```bash
./empty_file_finder.sh
```

---

# 💻 Sample Output

```text
Enter directory path:
/home/atika/Documents

Scanning...

Empty Files Found:

/home/atika/Documents/report.txt
/home/atika/Documents/notes/empty.doc

----------------------------------

Total Empty Files: 2
```

---

# 📚 Operating System Concepts Covered

- Shell Scripting
- File System Management
- Directory Traversal
- Linux Commands
- File Permissions
- Command-Line Utilities
- Automation

---

# 🚀 Future Improvements

- Delete empty files automatically
- Search by specific file extension
- Export results to a text file
- Interactive menu-driven interface
- Colorized terminal output
- Ignore hidden directories
- Progress indicator for large directories

---

# ⭐ Support

If you found this project useful, consider giving it a ⭐ on GitHub.

Your support encourages me to build and share more projects.

---

> *"Automation through shell scripting simplifies repetitive tasks and showcases the practical power of Operating Systems."*
