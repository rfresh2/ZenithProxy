import sys


def critical_error(*args):
    print("CRITICAL:", args)
    sys.exit(69)
