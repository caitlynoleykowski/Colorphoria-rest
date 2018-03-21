call gcloud config set project filler-staging
call mvn clean package -Pstaging
call mvn exec:java -DGetSwaggerDoc -Pstaging
call gcloud service-management deploy openapi.json
call mvn appengine:deploy -Pstaging
