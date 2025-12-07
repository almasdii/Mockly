# ML Processing Service

FastAPI service for processing interview audio files and generating analysis reports.

## Setup

```bash
# Install dependencies
pip install -r requirements.txt

# Run the service
python main.py

# Or with uvicorn directly
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## API Endpoints

### POST /api/process

Process audio file and generate interview analysis.

**Request:**
```json
{
  "sessionId": "uuid",
  "artifactId": "uuid",
  "artifactUrl": "https://...",
  "artifactType": "AUDIO_MIXED"
}
```

**Response:**
```json
{
  "metrics": {
    "score": 85.5,
    "clarity": 8.2,
    "confidence": 7.8,
    "pace": 6.5,
    "articulation": 8.0,
    "engagement": 7.5,
    "professionalism": 8.3,
    "technical_accuracy": 7.9
  },
  "summary": "...",
  "recommendations": "...",
  "transcript": {...}
}
```

### GET /health

Health check endpoint.

## Environment Variables

- `PORT`: Server port (default: 8000)

