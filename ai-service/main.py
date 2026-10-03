from fastapi import FastAPI

app = FastAPI(title="QA-Agent AI Service")


@app.get("/health")
def health():
    return {"status": "UP"}