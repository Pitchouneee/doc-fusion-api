# Document Fusion API

## Description

This Spring Boot application allows replacing merge fields in DOCX files and converting them to various output formats
(PDF by default) using JodConverter

## Features

- Replacing merge fields in DOCX documents
- Converting documents to different formats (PDF by default)
- Using JSON-based merge fields
- Simple REST API

## Prerequisites

- Java 21
- Docker
- Maven

## Dependencies

- Spring Boot 3.x
- Apache POI
- JodConverter
- Lombok
- Jackson

## Installation

### Clone the project

```bash
git clone https://github.com/Pitchouneee/doc-fusion-api.git
cd doc-fusion-api
```

#### Docker compose

```bash
docker-compose up --build
```

## Template document creation

### Merge field format

To use the document fusion API, you must create a DOCX file with specific merge fields.  

1. Open Microsoft Work or LibreOffice Writer
2. Create a new document
3. Enter text with merge fields between curly braces :

```
Last name : {lastname}
First name : {firstname}
City : {address.city}, {address.dep}
```

### Formatting guidelines

- Fields are case-sensitive
- Use dot notation for nested objects
- Curly braces `{}` delimit merge fields
- Ensure fields names exactly match your JSON

### Corresponding JSON example

```json
{
    "firstname": "Corentin", 
    "lastname": "BRINGER", 
    "address": { 
        "city": "Rodez", 
        "dep": "Aveyron" 
    }
}
```

## API usage

### Document generation endpoint

**URL** : `/api/v1/documents/generate`

**Méthode** : POST

**Content-Type** : `multipart/form-data`

**Parameters**:
- `file` : Source DOCX file (required)
- `data` : Merge data in JSON format (required)
- `outputFormat` : Output format (optional, default = PDF)

### Request example

```bash
curl -X POST -F "file=@template.docx" \
               -F 'data={"firstname":"Corentin","lastname":"BRINGER","address":{"city":"Rodez","dep":"Aveyron"}}' \
               -F "outputFormat=pdf" \
               http://host.docker.internal:8080/api/v1/documents/generate
```

## Configuration

Main parameters configurable via `application.properties` or environment variables

## Architecture

The project use a Docker Compose architecture with two services :

1. **Main application** :
    - Exposed port : 8080
    - Manage document fusion and transformation
    - Depend on conversion service

2. **JodConverter API** :
    - Exposed port : 8081
    - Document conversion service

### Network

A Docker bridge network is used for communication between services

## Deployment

Docker Compose configuration includes :
- Automatic image building
- Port configuration

## Contribution

Contributions are welcome ! If you want to report an issue or suggest feature, feel free to open an issue or submit a pull request