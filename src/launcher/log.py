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

def debug(msg: str):
    logger.debug(msg)

def debug_exception(msg: str):
    logger.debug(msg, exc_info=True)

def info(msg: str):
    logger.info(msg)

def warn(msg: str):
    logger.warning(msg)

def error(msg: str):
    logger.error(msg)

def exception(msg: str):
    logger.exception(msg)

def critical_error(msg: str):
    logger.critical(msg)
    sys.exit(69)

def critical_exception(msg: str):
    logger.critical(msg, exc_info=True)
    sys.exit(69)

def close():
    fh.close()
    ch.close()
