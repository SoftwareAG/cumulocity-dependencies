# Enable legacy serializer
    mongodb.json.parser.legacy=true 

# Remove feature toggle for mongo parse and serialize methods.
    1) Remove all class in src
    2) Remove maven-compiler-plugin from pom
    3) Replace com.cumulocity.repository.mongodb.util.DBObjectJsonSerializer.parse method with BasicDBObject.parse(json)
    4) Replace com.cumulocity.repository.mongodb.util.DBObjectJsonSerializer.serialize usage with its body.
    5) Remove com.cumulocity.repository.mongodb.util.DBObjectJsonSerializer and DBObjectJsonSerializerImpl and their use. 
    6) Remove mongodb.json.parser.legacy property.