call gcloud config set project colorphoria-staging
call mvn clean package -Pstaging
call mvn endpoints-framework:openApiDocs -Pstaging
call gcloud endpoints services deploy target/openapi-docs/openapi.json
call mvn appengine:deploy -Pstaging