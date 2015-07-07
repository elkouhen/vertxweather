import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.core.buffer.Buffer
import io.vertx.groovy.core.http.HttpClientResponse
import groovy.json.JsonSlurper
import io.vertx.core.Handler

String maShapeKey = 'oz5OTrdrQnmshayy6rDZZ0D7YCBCp16qCMhjsn8QxeG5h7mHCB'

def server = vertx.createHttpServer()

def router = Router.router(vertx)

router.route('/weather/:city').handler(
    { routingContext ->

        def city = routingContext.request().getParam('city')

        def response = routingContext.response()

        def client = vertx.createHttpClient([ssl: true, trustAll: true])

        def locationRequest = client.get(443, 'devru-latitude-longitude-find-v1.p.mashape.com',
				     "/latlon.php?location=${city}")

        locationRequest.putHeader('Accept', 'application/json')
        locationRequest.putHeader('X-Mashape-Key', maShapeKey)

        locationRequest.toObservable()
        .flatMap({locationResponse -> return locationResponse.toObservable() })
        .map({locationResponseData ->

                def json = new JsonSlurper().parseText("${locationResponseData.toString("UTF-8")}")

                def lat = json.Results[0].lat
                def lon = json.Results[0].lon

                return [lat, lon]
            })
        .flatMap ({data ->

                return rx.Observable.create({ subscriber ->

                        def weatherRequest = client.get(443, 'simple-weather.p.mashape.com',
				      "/weatherdata?lat=${data[0]}&lng=${data[1]}",
                            new Handler<HttpClientResponse>() {
                                public void handle(HttpClientResponse weatherResponse) {

                                    weatherResponse.bodyHandler(new Handler<Buffer>() {
                                            public void handle(Buffer weatherResponseData) {

                                                subscriber.onNext(weatherResponseData)
                                            }
                                        });
                                }
                            })

                        weatherRequest.putHeader('Accept', 'application/json')
                        weatherRequest.putHeader('X-Mashape-Key', maShapeKey)

                        weatherRequest.end()

                        return subscriber
                    })
            })
        .subscribe({weatherRespData ->

                def json = new JsonSlurper().parseText("${weatherRespData.toString("UTF-8")}")

                response.end("${json.query.results.channel.item.condition.text}")
            })

        locationRequest.end()
    })

server.requestHandler(router.&accept).listen(8080)

