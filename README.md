# Expense Tracker Receipt

Java Azure Function for receipt image extraction. The Expense Tracker API calls this function when `AI_PROVIDER=azure`.

## Local Setup

For local Azure Functions development, use `local.settings.json`. Azure Functions Core Tools loads this file automatically when running the function locally.

Install the local prerequisites:

- Java 17
- Maven, or use the included Maven wrapper
- Azure Functions Core Tools v4, which provides the `func` command
- Azurite, which provides local Azure Storage for `AzureWebJobsStorage=UseDevelopmentStorage=true`

Install and start Azurite with Docker:


```bash
docker run -d \
  --name expense-tracker-azurite \
  -p 10000:10000 \
  -p 10001:10001 \
  -p 10002:10002 \
  -v azurite-data:/data \
  mcr.microsoft.com/azure-storage/azurite

```

Azure Functions need to connect to Azure Storage. Locally, functions connect to the azurite emulator, even though the receipt function does not use storage. Ports 10000 (blob), 10001 (queue), 10002 (table).

Copy the sample settings file and add your Azure AI Document Intelligence resource values:

```bash
cp local.settings.json.sample local.settings.json
```

Required settings:

```json
{
  "Values": {
    "AZURE_DOCUMENT_AI_ENDPOINT": "https://your-resource.cognitiveservices.azure.com/",
    "AZURE_DOCUMENT_AI_KEY": "replace-me"
  }
}
```

Start the function locally:

```bash
./mvnw clean package
./mvnw azure-functions:run
```

If this project does not have a Maven wrapper yet, use:

```bash
mvn clean package
mvn azure-functions:run
```

The local endpoint is:

```text
http://localhost:7071/api/process-receipt
```

## API Contract

`POST /api/process-receipt`

Request:

- Body: raw receipt image bytes
- Headers:
  - `Content-Type: application/octet-stream`
  - `X-File-Name: receipt.jpg` optional
  - `X-Correlation-Id: <id>` optional
  - `x-functions-key: <key>` when calling a deployed function that requires a key

Response:

```json
{
  "description": "Starbucks",
  "amount": 15.50,
  "date": "2026-05-21",
  "category": "Food"
}
```

## Expense API Configuration

Run `expense-tracker-api` with:

```env
AI_PROVIDER=azure
RECEIPT_PROCESSOR_URL=http://localhost:7071/api/process-receipt
RECEIPT_PROCESSOR_FUNCTION_KEY=
```

For Azure deployment, set `RECEIPT_PROCESSOR_URL` to the deployed function URL and `RECEIPT_PROCESSOR_FUNCTION_KEY` to the function key.

## Azure Configuration

The Java code reads settings with `System.getenv(...)`. Locally, Azure Functions Core Tools maps values from `local.settings.json` into the function process. In Azure, configure the same values as Function App application settings:

```text
AzureWebJobsStorage=<storage-connection-string>
FUNCTIONS_WORKER_RUNTIME=java
AZURE_DOCUMENT_AI_ENDPOINT=https://your-resource.cognitiveservices.azure.com/
AZURE_DOCUMENT_AI_KEY=<document-intelligence-key>
```

Application settings are exposed to the Java function as environment variables, so the same code path works locally and in Azure.

Do not deploy `local.settings.json`. Keep secrets such as `AZURE_DOCUMENT_AI_KEY` out of source control. For production, prefer storing secrets in Azure Key Vault and referencing them from Function App application settings.

## Tests

```bash
mvn test
```
