"""Servicio externo de figuras. Ejecutar con Python 3, sin dependencias."""
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from functools import partial
from pathlib import Path

if __name__ == "__main__":
    handler = partial(SimpleHTTPRequestHandler, directory=str(Path(__file__).parent))
    print("Catálogo externo: http://127.0.0.1:8081/figuras/", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 8081), handler).serve_forever()
