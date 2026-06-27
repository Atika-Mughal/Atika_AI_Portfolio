#!/bin/bash
set -e

RED='\033[1;31m'; GREEN='\033[1;32m'
CYAN='\033[1;36m'; RESET='\033[0m'

echo -e "${GREEN}"
echo "╔══════════════════════════════════════════╗"
echo "║   Empty File Finder – Demo Script        ║"
echo "║   OS Project | C Language | Ubuntu       ║"
echo "╚══════════════════════════════════════════╝"
echo -e "${RESET}"

echo -e "${CYAN}▶ Step 1: Compiling...${RESET}"
make clean 2>/dev/null || true
make
echo -e "${GREEN}✔ Done!${RESET}"

echo -e "${CYAN}▶ Step 2: Setting up test directory...${RESET}"
make setup_test

echo -e "${CYAN}▶ Step 3: Basic scan:${RESET}"
./empty_finder test_dir || true

echo -e "${CYAN}▶ Step 4: Verbose scan:${RESET}"
./empty_finder -v test_dir || true

echo -e "${CYAN}▶ Step 5: Help screen:${RESET}"
./empty_finder -h

echo -e "${GREEN}All demos done!${RESET}"
