CC = gcc
CFLAGS = -Wall -std=c11 -O2
TARGET = empty_finder
SRC = src/empty_finder.c

all:	$(CC) $(CFLAGS) -o $(TARGET) $(SRC)

clean:	rm -f $(TARGET)

setup_test:	mkdir -p test_dir/subdir1/subdir2
		touch test_dir/empty1.txt
		touch test_dir/empty2.txt
		touch test_dir/subdir1/empty3.log
		echo "hello" > test_dir/has_content.txt
		touch test_dir/subdir1/subdir2/deep_empty.txt
		echo "data" > test_dir/subdir1/subdir2/deep_content.dat
