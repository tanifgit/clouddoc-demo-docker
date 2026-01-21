# InterSystems IRIS Cloud Document Java Demo (Docker)

This is a minimal end-to-end Java sample that connects to an InterSystems IRIS Cloud Document
deployment using TLS, creates a collection, inserts a few documents, lists them, and runs
a shorthand query.

It is packaged to run easily in Docker with configuration via environment variables.

## Files

- `pom.xml`                – Maven build with intersystems-document dependency and shade plugin
- `src/main/java/...`     – `CloudDocDemo.java` demo program
- `SSLConfig.properties`  – TLS configuration for the Java client
- `Dockerfile`            – Multi-stage build producing a runnable container
- `docker-entrypoint.sh`  – Imports the certificate into a keystore and runs the app
- `docker-compose.yml`    – Optional convenience wrapper
- `.env.sample`           – Sample environment variable file for docker compose

## 1. Prerequisites

- Docker (and optionally Docker Compose)
- Access to an InterSystems IRIS Cloud Document deployment with:
  - External connections enabled
  - Hostname, port, namespace, username, password
  - Downloaded X.509 certificate (PEM format)

## 2. Prepare your certificate

Download the X.509 certificate for your Cloud Document deployment from the Cloud Services Portal.
Save it somewhere on your host, for example:

```text
certs/certificateSQLaaS.pem
```
In this local folder.

You do NOT need to create a keystore on the host – the container will do that at startup.


## 3. Run with docker compose

1. Copy `.env.sample` to `.env` and edit it with your values:

   ```bash
   cp .env.sample .env
   # then edit .env
   ```

2. From the project root (where this README and Dockerfile live) run:

   ```bash
   docker compose up --build
   ```

The container will:

1. Import `/app/cert.pem` into `/app/keystore.jks` using `keytool`.
2. Use `SSLConfig.properties` (which points at `/app/keystore.jks`) for TLS.
3. Run the Java demo, which will:
   - Connect over TLS
   - Create/clear the collection
   - Insert a few documents
   - List them
   - Run a shorthand query
   - Exit

## 4. Build the Docker image (for plain docker run, see below)

From the project root (where this README and Dockerfile live):

```bash
docker build -t clouddoc-demo .
```

## 5. Run with a plain docker run

```bash
docker run --rm       -e IRIS_HOST="your-hostname.elb.us-west-2.amazonaws.com"       -e IRIS_PORT="443"       -e IRIS_NAMESPACE="USER"       -e IRIS_USER="SQLAdmin"       -e IRIS_PASSWORD="your-deployment-password"       -e COLLECTION_NAME="demoPeople"       -v /absolute/path/to/your/cert-file.pem:/app/cert.pem:ro       clouddoc-demo
```

Replace the values above with the connection information from your Cloud Services deployment
page, and the path to your downloaded certificate.




## 6. Local (non-Docker) run

You can also run the demo locally:

```bash
mvn clean package
mvn exec:java
```

For local runs, you must still create a Java keystore and update `SSLConfig.properties`
accordingly, as described also in the related article:

```bash
keytool -importcert -file /path/to/certs/cloud-document.pem -keystore keystore.jks
```
