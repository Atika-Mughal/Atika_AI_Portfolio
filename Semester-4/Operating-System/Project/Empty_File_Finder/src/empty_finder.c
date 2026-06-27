

#define _POSIX_C_SOURCE 200809L


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#define MAX_PATH   4096
#define MAX_DEPTH  64

#define COL_RESET  "\033[0m"
#define COL_RED    "\033[1;31m"
#define COL_GREEN  "\033[1;32m"
#define COL_YELLOW "\033[1;33m"
#define COL_CYAN   "\033[1;36m"
#define COL_BOLD   "\033[1m"

static long g_empty_count = 0;
static long g_total_files = 0;
static long g_total_dirs  = 0;
static int  g_use_colour  = 1;
static int  g_verbose     = 0;
static int  g_delete_mode = 0;

void print_banner(void);
void print_usage(const char *prog);
void scan_directory(const char *path, int depth);
void print_summary(void);

int main(int argc, char *argv[])
{
    const char *target_dir = ".";
    int opt;

    while ((opt = getopt(argc, argv, "vdnhH")) != -1) {
        switch (opt) {
            case 'v': g_verbose    = 1; break;
            case 'd': g_delete_mode = 1; break;
            case 'n': g_use_colour = 0; break;
            case 'h':
            case 'H':
                print_usage(argv[0]);
                return EXIT_SUCCESS;
            default:
                print_usage(argv[0]);
                return EXIT_FAILURE;
        }
    }

    if (optind < argc)
        target_dir = argv[optind];

    struct stat st;
    if (stat(target_dir, &st) != 0) {
        fprintf(stderr, COL_RED "Error: cannot stat '%s': %s\n" COL_RESET,
                target_dir, strerror(errno));
        return EXIT_FAILURE;
    }
    if (!S_ISDIR(st.st_mode)) {
        fprintf(stderr, COL_RED "Error: '%s' is not a directory.\n" COL_RESET,
                target_dir);
        return EXIT_FAILURE;
    }

    print_banner();

    if (g_use_colour)
        printf(COL_CYAN "Scanning: %s\n" COL_RESET, target_dir);
    else
        printf("Scanning: %s\n", target_dir);

    printf("%-60s  %s\n", "File Path", "Size");
    printf("%-60s  %s\n",
           "------------------------------------------------------------",
           "------");

    scan_directory(target_dir, 0);
    print_summary();

    return (g_empty_count > 0) ? 1 : EXIT_SUCCESS;
}

void scan_directory(const char *path, int depth)
{
    if (depth > MAX_DEPTH) {
        fprintf(stderr, COL_YELLOW
                "Warning: max depth %d reached at '%s'. Skipping deeper.\n"
                COL_RESET, MAX_DEPTH, path);
        return;
    }

    DIR *dir = opendir(path);
    if (!dir) {
        fprintf(stderr, COL_YELLOW "Warning: cannot open '%s': %s\n" COL_RESET,
                path, strerror(errno));
        return;
    }

    struct dirent *entry;
    char full_path[MAX_PATH];

    while ((entry = readdir(dir)) != NULL) {

        if (entry->d_name[0] == '.')
            continue;

        int written = snprintf(full_path, sizeof(full_path),
                               "%s/%s", path, entry->d_name);
        if (written < 0 || written >= (int)sizeof(full_path)) {
            fprintf(stderr, COL_YELLOW
                    "Warning: path too long, skipping '%s/%s'\n" COL_RESET,
                    path, entry->d_name);
            continue;
        }

        struct stat st;
        if (lstat(full_path, &st) != 0) {
            fprintf(stderr, COL_YELLOW
                    "Warning: cannot stat '%s': %s\n" COL_RESET,
                    full_path, strerror(errno));
            continue;
        }

        if (S_ISREG(st.st_mode)) {
            g_total_files++;

            if (g_verbose && g_use_colour)
                printf(COL_BOLD "  Checking: " COL_RESET "%s\n", full_path);
            else if (g_verbose)
                printf("  Checking: %s\n", full_path);

            if (st.st_size == 0) {
                g_empty_count++;

                if (g_use_colour)
                    printf(COL_RED "%-60s  0 bytes\n" COL_RESET, full_path);
                else
                    printf("%-60s  0 bytes\n", full_path);

                if (g_delete_mode) {
                    if (unlink(full_path) == 0)
                        printf("  -> Deleted.\n");
                    else
                        fprintf(stderr, "  -> Delete failed: %s\n", strerror(errno));
                }
            }

        } else if (S_ISDIR(st.st_mode)) {
            g_total_dirs++;
            scan_directory(full_path, depth + 1);
        }
    }

    closedir(dir);
}

void print_banner(void)
{
    if (g_use_colour) {
        printf(COL_GREEN
               "\n╔══════════════════════════════════════╗\n"
               "║      EMPTY FILE FINDER  v1.0         ║\n"
               "║  Operating Systems Project — C Lang  ║\n"
               "╚══════════════════════════════════════╝\n\n"
               COL_RESET);
    } else {
        printf("\n=== EMPTY FILE FINDER v1.0 ===\n"
               "Operating Systems Project — C Language\n\n");
    }
}

void print_summary(void)
{
    printf("\n%-60s  %s\n",
           "============================================================",
           "======");
    if (g_use_colour) {
        printf(COL_BOLD "\nSUMMARY\n-------\n" COL_RESET);
        printf("  Directories visited : " COL_CYAN "%ld\n" COL_RESET, g_total_dirs);
        printf("  Regular files scanned: " COL_CYAN "%ld\n" COL_RESET, g_total_files);
        if (g_empty_count == 0)
            printf("  Empty files found  : " COL_GREEN "0 (none)\n" COL_RESET);
        else
            printf("  Empty files found  : " COL_RED "%ld\n" COL_RESET, g_empty_count);
    } else {
        printf("\nSUMMARY\n-------\n");
        printf("  Directories visited  : %ld\n", g_total_dirs);
        printf("  Regular files scanned: %ld\n", g_total_files);
        printf("  Empty files found    : %ld\n", g_empty_count);
    }
    printf("\n");
}

void print_usage(const char *prog)
{
    printf("\nUsage: %s [OPTIONS] [DIRECTORY]\n\n", prog);
    printf("OPTIONS:\n");
    printf("  -v   Verbose mode\n");
    printf("  -d   Delete mode: remove empty files\n");
    printf("  -n   No colour output\n");
    printf("  -h   Show this help\n\n");
    printf("EXAMPLES:\n");
    printf("  %s\n", prog);
    printf("  %s /home/user/docs\n", prog);
    printf("  %s -v /tmp\n", prog);
    printf("  %s -d /tmp/cache\n\n", prog);
}
