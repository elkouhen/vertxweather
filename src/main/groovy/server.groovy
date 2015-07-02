import io.vertx.groovy.ext.web.Router
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import groovy.json.JsonSlurper

def server = vertx.createHttpServer()

def router = Router.router(vertx)

def route = router.route('/weather/:city')
.handler(
  { routingContext ->

    def city = routingContext.request().getParam('city')

    def response = routingContext.response() 

    def client = vertx.createHttpClient([ssl: true, trustAll: true]) 

    def request = client
    .get(443, 'devru-latitude-longitude-find-v1.p.mashape.com', 
	 "/latlon.php?location=${city}",
	 { resp ->
	   
	   resp.bodyHandler(
	     { body ->
	       def json=new JsonSlurper().parseText("${body.toString('ISO-8859-1')}")

	       def lat = json.Results[0].lat;
	       def lon = json.Results[0].lon; 
	  
	       response.end("${lat}${lon}")
	     })
	 })
    
    request.putHeader('Accept', 'application/json')
    request.putHeader('X-Mashape-Key', 'oz5OTrdrQnmshayy6rDZZ0D7YCBCp16qCMhjsn8QxeG5h7mHCB')
    
    request.end()
  })

server.requestHandler(router.&accept).listen(8080)
