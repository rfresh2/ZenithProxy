import logging.handlers
import os
import sys


logger = logging.getLogger("log")
logger.setLevel(logging.DEBUG)
ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
logger.addHandler(ch)
console_formatter = logging.Formatter("%(message)s")
ch.setFormatter(console_formatter)
os.makedirs("log", exist_ok=True) # create subfolder if needed
fh = logging.handlers.RotatingFileHandler("log/launcher.log", maxBytes=1_000_000, backupCount=5)
fh.setLevel(logging.DEBUG)
logger.addHandler(fh)
file_formatter = logging.Formatter("[%(asctime)s] [%(levelname)s] %(message)s", "%Y/%m/%d %H:%M:%S")
fh.setFormatter(file_formatter)
fh.doRollover()

def debug(*args):
    logger.debug(*args)

def info(*args):
    logger.info(*args)

def warn(*args):
    logger.warning(*args)

def error(*args):
    logger.error(*args)

def critical_error(*args):
    logger.critical(*args)
    sys.exit(69)

def close():
    fh.close()
    ch.close()
