// Wait for a known process to exit or for the next scheduled service check.
// This program observes processes. It never signals them or changes app state.
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <poll.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/inotify.h>
#include <sys/syscall.h>
#include <time.h>
#include <unistd.h>

#define FALLBACK 125
#define CONTROL_CHANGED 12

static int64_t milliseconds(void) {
    struct timespec now;
    if (clock_gettime(CLOCK_MONOTONIC, &now) < 0) return -1;
    return (int64_t)now.tv_sec * 1000 + now.tv_nsec / 1000000;
}

static long number(const char *text) {
    char *end;
    errno = 0;
    long value = strtol(text, &end, 10);
    return errno || !*text || *end ? -1 : value;
}

static int control_changed(int directory) {
    if (faccessat(directory, "health-now", F_OK, 0) == 0) return 1;
    return faccessat(directory, "health-enabled", F_OK, 0) < 0 && errno == ENOENT;
}

int main(int argc, char **argv) {
#ifndef __NR_pidfd_open
    return FALLBACK;
#else
    if (argc != 5) return FALLBACK;
    long seconds = number(argv[4]);
    if (seconds < 1 || seconds > 10) return FALLBACK;
    struct pollfd entries[3] = {{-1, POLLIN, 0}, {-1, POLLIN, 0}, {-1, POLLIN, 0}};
    for (int i = 0; i < 2; ++i) {
        long pid = number(argv[i + 2]);
        if (pid == 0) continue; // Optional Shizuku PID.
        if (pid < 2 || pid > INT_MAX) return FALLBACK;
        entries[i].fd = (int)syscall(__NR_pidfd_open, (int)pid, 0);
        if (entries[i].fd < 0) return errno == ESRCH ? 10 + i : FALLBACK;
    }
    if (entries[0].fd < 0) return FALLBACK;
    int directory = open(argv[1], O_RDONLY | O_DIRECTORY | O_CLOEXEC | O_NOFOLLOW);
    if (directory < 0) return FALLBACK;
    entries[2].fd = inotify_init1(IN_CLOEXEC | IN_NONBLOCK);
    if (entries[2].fd < 0 || inotify_add_watch(entries[2].fd, argv[1],
            IN_CREATE | IN_DELETE | IN_MOVED_TO | IN_MOVED_FROM | IN_ATTRIB) < 0) return FALLBACK;
    int64_t start = milliseconds();
    if (start < 0) return FALLBACK;
    int64_t deadline = start + seconds * 1000;
    for (;;) {
        if (control_changed(directory)) return CONTROL_CHANGED;
        int64_t now = milliseconds();
        if (now < 0) return FALLBACK;
        if (now >= deadline) return 0;
        int result = poll(entries, 3, (int)(deadline - now));
        if (result < 0 && errno == EINTR) continue;
        if (result < 0) return FALLBACK;
        if (result == 0) return 0;
        for (int i = 0; i < 3; ++i) {
            if (entries[i].revents & (POLLERR | POLLNVAL)) return FALLBACK;
        }
        if (entries[0].revents & (POLLIN | POLLHUP)) return 10;
        if (entries[1].revents & (POLLIN | POLLHUP)) return 11;
        if (entries[2].revents & POLLHUP) return FALLBACK;
        char events[4096];
        if (entries[2].revents & POLLIN) {
            if (read(entries[2].fd, events, sizeof(events)) < 0 && errno != EAGAIN && errno != EINTR)
                return FALLBACK;
        }
    }
#endif
}
