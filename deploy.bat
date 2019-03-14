call gcloud config set project colorphoria-app
call mvn -U clean package -Pprod
call mvn endpoints-framework:openApiDocs -Pprod
call gcloud endpoints services deploy target/openapi-docs/openapi.json
call mvn appengine:deploy -Pprod
