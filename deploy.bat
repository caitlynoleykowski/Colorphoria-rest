call gcloud config set project filler-ea8ae
call mvn clean package -Pprod
call mvn exec:java -DGetSwaggerDoc -Pprod
call gcloud service-management deploy openapi.json
call mvn appengine:deploy -Pprod
