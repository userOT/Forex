###### How to start application.

1) Docker. Use docker-compose up --build to start the application. 
  This command will start 2 containers - one-frame and application itself.
Please note that in this approach need to wait until all dependencies will be resolved.
2) If you don't want to wait, you can start only one-frame docker container and application could be started from terminal by using sbt run. Please note that first you need to change application.conf and uncomment line 381 insted of line 380 to use direct url.
3) Go to port 9000 and trigger the api

`docker-compose up --build
`
OR

`sbt run`

###### How to use API

`http://localhost:9000/rates?pair=USDEUR&pair=EURUSD
`

###### How 1000 connections problem is solving
One-Frame API gives us ability to request multiple rates per request.
I used very simple caching to solve this issue. All available currencies that application should be combined and send as a single request to 
one-frame service every 4 minutes. If we have only 1000 requests per token daily and keep 5 min refreshing, we can count the frequency. 1000 / 24 hours = 41 connections per hour / 60 min = 0.69 per minute * 4 = approx 2.
So basically, we can create new connection every 2 minutes ans update the cache. I used 4 min to save some connections.

So when application is starting, it takes all supported currencies and create the cache.
When client requests rate, it goes not to one-frame and foes to cache and get the same result not older than 5 min.

###### What I used
1) Scala 2.12 (Could be update to latest one)
2) Play Framework 2.8 as rest client
3) Dependency injection using Guice
4) Internal play scheduler to request one-frame time to time
5) ScalaTest
6) Mockito
7) Play WsClient to reach external services
8) Full Futures support. Non-blocking and everything async
9) No exceptions throwing
10) Layered architecture

###### Why I didn't change existing code

It looks for me without domain specific, and I would spend much time for refactoring

###### What I missed in implementation

1) Failovers and retries for one-frame requests
2) Maybe some test cases
3) I used a single cache instead of currency specific, it could be not so giant and update only specific currency
4) Cache should be not in memory and something like Redis or SQL DB.