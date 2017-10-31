call mvn clean package
call mvn exec:java -DGetSwaggerDoc
call gcloud service-management deploy openapi.json
call mvn appengine:deploy
