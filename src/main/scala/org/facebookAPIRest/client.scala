package org.facebookAPIRest

import spray.json._
import java.net.URLEncoder
import java.security.SecureRandom
import javax.crypto.{SecretKey, Cipher, KeyGenerator}
import java.security._
import java.util.Base64
import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}
import akka.actor._
import spray.json.{pimpAny, DefaultJsonProtocol}
import spray.routing.SimpleRoutingApp
import scala.collection.mutable
import scala.collection.mutable.{SynchronizedMap, ListBuffer}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.util.{Success, Random}
import akka.actor.Props
import scala.io.Source
import spray.client.pipelining._

/* Functions implemented Server side:

GET
___

-getStats()
-listUsers()
-listPages()
-getposts(userId)
-getfriends(userId)
-viewPage(pageId)
-viewProfile(userId)
-getNotifications(userId)


POST
____

-create(userId,fname,lname,age,sex,location,occupation,interestedIn)
-addfriend(friend1,friend2)
-createPage(userId,pageId,desc)
-subscribeToPage(userId,pageId)
-post(userId,message)
-postToPage(userId,pageId,message)
-postToUser(userId,friendId,message)
-stopSystem()

*/

/*Actor Methods*/
case class Start()
case class Stop()
case class create_user_client(userId:String,fname:String,lname:String,age:String,sex:String,location:String,occupation:String,interestedIn:String)
case class create_page_client(userId:String, pageId:String,desc:String)
case class add_friend_client(userId:String,friendId:String)
case class subscribe_client(userId:String,pageId:String)
//case class post_to_own_wall(userId:String,message:String)
case class post_to_own_wall(userId:String,aesEncMessage:String,iv:String,pubKeyEncAesList:ListBuffer[String])
case class post_to_page(userId:String,pageId:String,message:String)
//case class post_to_friend(userId:String,friendId:String,message:String)
case class post_to_friend(userId:String,friendId:String,aesEncMessage:String,iv:String,pubKeyEncAesList:ListBuffer[String])
case class getOwnPosts(userId:String,friendId:String)
case class startWork()

/*Data Classes*/
case class post_msg_enc(var userID:String,var Msg:String,var IV:String,var encrypted_keys:Seq[String])
case class post_to_friend_enc(var userID:String,var friendID:String,var Msg:String,var IV:String,var encrypted_keys:Seq[String])
case class wallPosts(var msg:String,var iv:String,var key:String)
case class initAuthStepTwoClass(var userId:String,var sRNGEnc:String)
case class userPubKeyClass(var userId:String,publicKeyString:String)

object userPubKeyClassJsonProtocol extends DefaultJsonProtocol {
  implicit val userPubKeyClassJsonFormat=jsonFormat2(userPubKeyClass)
}

object initAuthStepTwoClassJsonProtocol extends DefaultJsonProtocol {
  implicit val initAuthStepTwoClassJsonFormat=jsonFormat2(initAuthStepTwoClass)
}

object postMsgJsonProtocol extends DefaultJsonProtocol {
  implicit val postMsgJsonFormat=jsonFormat4(post_msg_enc)
}

object postToFriendJsonProtocol extends DefaultJsonProtocol {
  implicit val postToFriendJsonFormat=jsonFormat5(post_to_friend_enc)
}

object wallPostsGetJsonProtocol extends DefaultJsonProtocol {
  implicit val wallPostsGetJsonFormat=jsonFormat3(wallPosts)
}

object FB_Client extends App with SimpleRoutingApp{

  import postMsgJsonProtocol._
  import postToFriendJsonProtocol._
  import wallPostsGetJsonProtocol._
  import initAuthStepTwoClassJsonProtocol._
  import userPubKeyClassJsonProtocol._

  var totalUsers=400
  totalUsers = totalUsers + (3-(totalUsers%3))
  val totalPages=400
  val distinctPosts=200
  var maxFrndReqPerUser = math.ceil(0.35 * totalUsers).toInt/*~35% of totalUsers*/

  //var usersMap = new HashMap[String,FBUser]
  var pubKeyMap=new mutable.HashMap[String,PublicKey] () with SynchronizedMap[String,PublicKey]
  val random_page_creators = Array.ofDim[Int](totalPages)
  val distinct_data = 1000
  val first_names = Source.fromFile("first_names.txt").getLines().toList
  //println(first_names.size)
  val last_names = Source.fromFile("last_names.txt").getLines().toList
  //println(last_names.size)
  val occupation = Source.fromFile("occupations.txt").getLines().toList
  //println(occupation.size)
  val location = Source.fromFile("locations.txt").getLines().toList
  //println(location.size)
  val age = Source.fromFile("ages.txt").getLines().toList
  //println(age.size)
  val pageTitles = Source.fromFile("page_titles.txt").getLines().toList
  //println(pageTitles.size)
  val posts = Source.fromFile("posts.txt").getLines().toList
  //println(posts)
  val gender:List[String] = List("Male","Female")
  //println(gender.size)

  val create_user = "http://localhost:8080/create?"
  //s"userId=$&fname=$&lname=$&age=$&sex=$&location=$&occupation=$&interestedIn=$"
  val add_friend = "http://localhost:8080/addfriend?"
  //s"friend1=$&friend2=$"
  val post_message = "http://localhost:8080/post?"
  //s"userId=$&message=$"
  val post_msg = "http://localhost:8080/post_msg?"
  //s"post_msg_json="
  val getPosts = "http://localhost:8080/getposts?"
  //s"userId=$"
  val getFriends = "http://localhost:8080/getfriends?"
  //s"userId=$"
  val createPage = "http://localhost:8080/createPage?"
  //s"userId=$&pageId=$&desc=$"
  val subscribe = "http://localhost:8080/subscribeToPage?"
  //s"userId=$&pageId=$"
  val postToPage = "http://localhost:8080/postToPage?"
  //s"userId=$&pageId=$&message=$"
  val postToUser = "http://localhost:8080/postToUser?"
  //s"userId=$&friendId=$&message=$"
  val post_msg_friend = "http://localhost:8080/post_to_friend?"
  //s"post_to_friend_json=$"
  val viewPage = "http://localhost:8080/viewPage?"
  //s"pageId=$"
  val viewProfile = "http://localhost:8080/viewProfile?"
  //s"userId=$"
  val getNotifications = "http://localhost:8080/getNotifications?"
  //s"userId=$
  val getWall = "http://localhost:8080/get_wall_posts?"
  //s"userid=$&friendid=$"
  val systemShutdown = "http://localhost:8080/stopServer"
  val initAuthPath = "http://localhost:8080/initClient"
  val initAuthPathTwo = "http://localhost:8080/initClientStepTwo?"
  //s"stepTwoJson=$"
  val userPubKeyPath = "http://localhost:8080/userPubKeyPath?"
  //s"userPubKeyJson=$"

  val gender_rng= Random


  val listUsers = "http://localhost:8080/listUsers"
  val listPages = "http://localhost:8080/listPages"
  val getStats = "http://localhost:8080/getStats"


  implicit val actorsystem = ActorSystem()
  import actorsystem.dispatcher
  val pipeline=sendReceive
  var getStatsFuncCancellable: Cancellable = null

  for(i <- 0 to totalUsers-1){
    val client_actor = actorsystem.actorOf(Props(new Client_Actor(i,totalUsers-1,actorsystem)),s"Actor_$i")
    client_actor ! Start()
  }



  //println("Reached to get stats cancellable")
  //getStatsFuncCancellable = actorsystem.scheduler.schedule(Duration.create(10, TimeUnit.MILLISECONDS),Duration.create(200, TimeUnit.MILLISECONDS))(getStatsFunc)

  def getStatsFunc(){
    //println("Inside")
    val status=pipeline(Get(getStats))
    status.foreach{
      response => println(response.entity.asString)
    }
  }

  Thread.sleep(60000)

  for(i <- 0 to totalUsers-1){
    var actor=actorsystem.actorSelection("/user/Actor_"+i)
    actor ! Stop()
  }
  Thread.sleep(200)
//  getStatsFuncCancellable.cancel()
  pipeline(Post(systemShutdown))
  //pipeline
  //Thread.sleep(200)
  //println("Server told to close.")
  actorsystem.shutdown()

  /*FB User Actor*/
  class Client_Actor(i:Int,totalUsers:Int,actorsystem:ActorSystem) extends Actor{

    import actorsystem.dispatcher
    val actor_pipeline=sendReceive
    val maxUserId=totalUsers
    val thisUserId=i.toString
    var pageIdList = new ListBuffer[String]()
    var friendIdList = new ListBuffer[String]()
    var pageOwned:String = new String()
    var userType:String = new String()
    //var postsCancellable: Cancellable = null
    var ownWallPostCancellable:Cancellable = null
    var postToUserCancellable:Cancellable = null
    var postToPageCancellable:Cancellable = null

    var fetchWall:Cancellable = null
    var postcount:Int=0
    var isAuthenticated:Int = 0
    if(i<=(maxUserId+1)/3){
      userType = "Active"
    }else if(i>(maxUserId+1)/3 && i<=((maxUserId+1)*2)/3){
      userType = "Intermittent"
    }else{
      userType = "Dormant"
    }

    var noOfFrndReq=Random.nextInt(maxFrndReqPerUser)
    while (noOfFrndReq == 0){
      noOfFrndReq=Random.nextInt(maxFrndReqPerUser)
    }

    /*I-V generation*/
    var sRNG = SecureRandom.getInstance("SHA1PRNG")
    sRNG.setSeed(sRNG.generateSeed(32))
    var initVector = new Array[Byte](16)
    sRNG.nextBytes(initVector)

    /*Public-Private Key generation*/
    var keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(512)
    var keypair = keyGen.genKeyPair
    var pvtKey = keypair.getPrivate
    var pubKey = keypair.getPublic
    pubKeyMap.put(thisUserId,pubKey)


    def receive = {
      case "Hello" => println("Hello")
      case Start() => {
        var pubkeyinstring=Base64.getEncoder.encodeToString(pubKey.getEncoded)
        var userPubKey = userPubKeyClass(thisUserId,pubkeyinstring)
        var userPubKeyJson = URLEncoder.encode(userPubKey.toJson+"","UTF-8")
        actor_pipeline(Post(userPubKeyPath+s"userPubKeyJson=$userPubKeyJson"))


        //self ! sendPublicKey(thisUserId,pubkeyinstring)

        self ! create_user_client(thisUserId,first_names(Random.nextInt(distinct_data)),last_names(Random.nextInt(distinct_data)),age(Random.nextInt(distinct_data)),gender(gender_rng.nextInt(2)),location(Random.nextInt(distinct_data)),occupation(Random.nextInt(distinct_data)),gender(gender_rng.nextInt(2)))
        //Thread.sleep(200)
        //println("User Created")
        /* If 0<=Actor_Number<=399 , it shall create a page*/

        //while(isAuthenticated==0){
          //println("initAuthStepOne-1")
          var initAuthStepOne=actor_pipeline(Post(initAuthPath))
          var serversRNG:String =""
          initAuthStepOne.foreach{
            result => {
              //println("initAuthStepOne-2")
              serversRNG = (s"${result.entity.data.asString}")
              //println("Server's sRNG : "+serversRNG)

              var auth_pvtKeyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
              auth_pvtKeyCipher.init(Cipher.ENCRYPT_MODE,pvtKey)
              var enc_server_sRNG = auth_pvtKeyCipher.doFinal(serversRNG.getBytes())
              var enc_server_sRNG_string = Base64.getEncoder.encodeToString(enc_server_sRNG)

              var authstepTwoJson = URLEncoder.encode(initAuthStepTwoClass(thisUserId,enc_server_sRNG_string).toJson+"","UTF-8")
              var initAuthStepTwo=actor_pipeline(Post(initAuthPathTwo+s"stepTwoJson=$authstepTwoJson"))

              var resultAuth:String = ""
              //println(initAuthStepTwo)
              initAuthStepTwo.foreach{
                resulttwo => {
                  resultAuth=(s"${resulttwo.entity.asString}")
                  //println(resultAuth)
                  if(resultAuth.equalsIgnoreCase("Authenticated")){
                    println(s"User Id $thisUserId Authenticated")
                    isAuthenticated = 1
                    self ! startWork()
                  }
                }
              }
            }
          }
        //}
      }
      case startWork() =>{

        if(i>=0 && i<=totalPages-1){
          self ! create_page_client(thisUserId,thisUserId,pageTitles(i))
        }

        /*Add friends*/
        for(k <- 1 to noOfFrndReq){
          var random_friend = Random.nextInt(totalUsers)

          while (random_friend == i){
            random_friend = Random.nextInt(totalUsers)
          }
          while (friendIdList.contains(random_friend.toString)){
            random_friend = Random.nextInt(totalUsers)
          }
          self ! add_friend_client(thisUserId,random_friend.toString)
        }



        /*Subscribe to Page - Avg. user likes 40 pages. Hence RNG range is from 30 to 50. Avg. of this distribution is 40*/

        var totalPagesToBeLiked=Random.nextInt(20)+30
        if(pageOwned != null){
          //Actual likes will be totalPagesToBeLiked-1 because the user himself created a page
          totalPagesToBeLiked = totalPagesToBeLiked - 1
        }
        for (i <- 1 to totalPagesToBeLiked){
          var pageToBeLiked = Random.nextInt(totalPages)
          while (pageIdList.contains(pageToBeLiked.toString) || pageOwned.equals(pageToBeLiked.toString)){
            pageToBeLiked = Random.nextInt(totalPages)
          }
          self ! subscribe_client(i.toString,pageToBeLiked.toString)
        }

        /*User behaviour simulation:

        * The users have been equally distributed among Very Active, Intermittently Active, and Dormant users.

        * For Very Active users => status updates are scheduled every 300ms.
        *                          posts to friends' walls are scheduled every 700ms.
        *                          posts to subscribed pages' walls are scheduled every 1000ms.

        * For Intermittently Active users => status updates are scheduled every 1500ms.
        *                                    posts to friends' walls are scheduled every 3500ms.
        *                                    posts to subscribed pages' walls are scheduled every 5000ms.
        *
        * For Dormant users => status updates are scheduled every 3000ms.
        *                      posts to friends' walls are scheduled every 7000ms.
        *                      posts to subscribed pages' walls are scheduled every 10000ms.
        *
        * */
        if(userType.equalsIgnoreCase("Active")){
          //ownWallPostCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(300, TimeUnit.MILLISECONDS))(wallPostFunc)
          postToUserCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(700, TimeUnit.MILLISECONDS))(postToUserFunc)
          //postToPageCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(1000, TimeUnit.MILLISECONDS))(postToPageFunc)
        }
        if(userType.equalsIgnoreCase("Intermittent")){
          //ownWallPostCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(1500, TimeUnit.MILLISECONDS))(wallPostFunc)
          postToUserCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(3500, TimeUnit.MILLISECONDS))(postToUserFunc)
          //postToPageCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(5000, TimeUnit.MILLISECONDS))(postToPageFunc)
        }
        if(userType.equalsIgnoreCase("Dormant")){
          //ownWallPostCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(3000, TimeUnit.MILLISECONDS))(wallPostFunc)
          postToUserCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(7000, TimeUnit.MILLISECONDS))(postToUserFunc)
          //postToPageCancellable = context.system.scheduler.schedule(Duration.create(100, TimeUnit.MILLISECONDS),Duration.create(10000, TimeUnit.MILLISECONDS))(postToPageFunc)
        }

        /*Fetching wall of user with user id=10
        * */
        if(i==10){
          fetchWall = context.system.scheduler.schedule(Duration.create(10000, TimeUnit.MILLISECONDS),Duration.create(5000, TimeUnit.MILLISECONDS))(fetchWallFunc)
        }
      }
      case create_user_client(userId:String,fname:String,lname:String,age:String,sex:String,location:String,occupation:String,interestedIn:String) => {
        //println("reached create_user_client")
        var response = actor_pipeline(Post(create_user+s"userId=$userId&fname=$fname&lname=$lname&age=$age&sex=$sex&location=$location&occupation=$occupation&interestedIn=$interestedIn".toString))
        //response.foreach{value => println(value.entity.asString)}
      }
      case create_page_client(userId:String, pageId:String,desc:String) => {
        //println("Reached create_page_client")
        var response=actor_pipeline(Post(createPage+s"userId=$userId&pageId=$pageId&desc=$desc".toString))
        //response.foreach{value => println(value.entity.asString)}
        pageOwned = pageId
      }
      case add_friend_client(userId:String,friendId:String) => {
        var response=actor_pipeline(Post(add_friend+s"friend1=$userId&friend2=$friendId".toString))
        //response.foreach{value => println(value.entity.asString)}
        friendIdList += friendId
      }
      case subscribe_client(userId:String,pageId:String) => {
        var response=actor_pipeline(Post(subscribe+s"userId=$userId&pageId=$pageId".toString))
        //response.foreach{value => println(value.entity.asString)}
        pageIdList += pageId
      }
//      case post_to_own_wall(userId:String,message:String) => {
//        var response=actor_pipeline(Post(post_message+s"userId=$userId&message=$message".toString))
//        //response.foreach{value => println(value.entity.asString)}
//      }
      case post_to_own_wall(userId:String,aesEncMessage:String,iv:String,pubKeyEncAesList:ListBuffer[String]) => {
//        if(i==10){
//          //println("Posting encrypted message to own wall :"+aesEncMessage)
//        }
        var request = URLEncoder.encode(post_msg_enc(userId,aesEncMessage,iv,pubKeyEncAesList).toJson+"","UTF-8")
        var response=actor_pipeline(Post(post_msg+"post_msg_json="+request))
        postcount +=1
      }
      case post_to_page(userId:String,pageId:String,message:String) => {
        var response=actor_pipeline(Post(postToPage+s"userId=$userId&pageId=$pageId&message=$message".toString))
        //response.foreach{value => println(value.entity.asString)}
      }
//      case post_to_friend(userId:String,friendId:String,message:String) => {
//        var response=actor_pipeline(Post(postToUser+s"userId=$userId&friendId=$friendId&message=$message".toString))
//        //response.foreach{value => println(value.entity.asString)}
//      }
      case post_to_friend(userId:String,friendId:String,aesEncMessage:String,iv:String,pubKeyEncAesList:ListBuffer[String]) => {
//        if(friendId.toInt == 10){
//          //println("Posting encrypted message to user 10's wall :"+aesEncMessage)
//          //println("Posting iv to user 10's wall :"+iv)
//          //println(pubKeyEncAesList)
//          //postcount += 1
//        }
        var ptf_request = URLEncoder.encode(post_to_friend_enc(userId,friendId,aesEncMessage,iv,pubKeyEncAesList).toJson+"","UTF-8")
        var response=actor_pipeline(Post(post_msg_friend+"post_to_friend_json="+ptf_request))
        //response.foreach{value => println(value.entity.asString)}
        postcount += 1
      }
      case getOwnPosts(userId:String,friendId:String) => {
        //println("-----------GetOwnPost UserId 10-----------")
        import wallPostsGetJsonProtocol._
        var wallPostGetCipher_RSA = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        var wallPostGetCipher_AES = Cipher.getInstance("AES/CBC/PKCS5Padding")
        var response = actor_pipeline(Get(getWall+s"userid=$userId&friendid=$friendId"))

        //response.foreach{value => println(value.entity.asString)}
        response onComplete {
          case Success(result) => {
            var listOfWallPosts = result.entity.data.asString.parseJson.convertTo[List[wallPosts]]
            //println("length of List of retrieved wall posts :"+listOfWallPosts.length)
            if(listOfWallPosts.length>0){
              println("---------------User 10's Wall------------------")
              for (wallPost <- listOfWallPosts){
                //println(wallPost)
                //println("pvtKey :"+Base64.getEncoder.encodeToString(pvtKey.getEncoded))
                wallPostGetCipher_RSA.init(Cipher.DECRYPT_MODE, pvtKey)

                var aeskey=wallPost.key.map(c => if(c == ' ') '+' else c)
                var iv=wallPost.iv.map(c => if(c == ' ') '+' else c)
                var msg=wallPost.msg.map(c => if(c == ' ') '+' else c)

                var origAesKey_ByteArray = Base64.getDecoder.decode(new String(wallPostGetCipher_RSA.doFinal(Base64.getDecoder.decode(aeskey))))

                var origAesKey:SecretKey = new SecretKeySpec(origAesKey_ByteArray,0,origAesKey_ByteArray.length,"AES")
                //System.arraycopy(var1, var2, this.key, 0, var3);
                wallPostGetCipher_AES.init(Cipher.DECRYPT_MODE, origAesKey, new IvParameterSpec(Base64.getDecoder.decode(iv)))
                println("Encrypted Post : "+msg)
                println("Decrypted Post : "+new String(wallPostGetCipher_AES.doFinal(Base64.getDecoder.decode(msg))).map(c => if(c == '+') ' ' else c)+"\n")
              }
              println("-----------------------------------------------")
            }
          }
        }
      }
      case Stop() => {
        //ownWallPostCancellable.cancel()
        postToUserCancellable.cancel()
        //postToPageCancellable.cancel()
//        fetchWall.cancel()
        context.stop(self)
      }
    }

    def fetchWallFunc(){
      //println("Encrypted : Inside fetchWallFunc")
      //println(postcount)
      if(postcount>0){
        //println("**************Encrypted : Inside fetchWallFunc as postcount more than 0")
        self ! getOwnPosts(thisUserId,thisUserId)
      }
    }

    def wallPostFunc(){
      var postText = posts(Random.nextInt(distinctPosts))
      var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

      var aesKeyGenerator = KeyGenerator.getInstance("AES")
      aesKeyGenerator.init(128)
      var aesKey = aesKeyGenerator.generateKey()

      cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(initVector))
/*
*   //create new key
    SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
    //get base64 encoded version of the key
    String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

    //decode the base64 encoded string
    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
    //rebuild key using SecretKeySpec
    SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
*/
      var aesKeyinString = Base64.getEncoder().encodeToString(aesKey.getEncoded)
      var encryptedMessage = cipher.doFinal(postText.getBytes("UTF-8"))
      var encryptedMessageString = Base64.getEncoder.encodeToString(encryptedMessage)
      var initVectorString = Base64.getEncoder.encodeToString(cipher.getIV)

//      var wait_count_friendIdList:Int=0
//      while(!(friendIdList.length>0)){
//        wait_count_friendIdList+=1
//      }
      var noOfFriendsForPost=10 //Random.nextInt(friendIdList.length)
      if(noOfFriendsForPost == 0){
        noOfFriendsForPost=1
      }
      var aesEncPubList = new ListBuffer[String]()
      /*Inserting AES key encrypted with own public key*/
      var own_pubKeyCipher_AES = Cipher.getInstance("RSA/ECB/PKCS1Padding")
      own_pubKeyCipher_AES.init(Cipher.ENCRYPT_MODE,pubKey)
      var own_encryptedAesPublicKey = own_pubKeyCipher_AES.doFinal(aesKeyinString.getBytes("UTF-8"))
      var own_encryptedAesPublicKeyString = Base64.getEncoder.encodeToString(own_encryptedAesPublicKey)
      var own_aesEncWPub = thisUserId+"!"+own_encryptedAesPublicKeyString
      aesEncPubList += own_aesEncWPub
      var frndsWithAccess = new ListBuffer[String]()
      if(friendIdList.length>0){
        for (i <- 1 to noOfFriendsForPost){
          var rndmfrndid_aes = friendIdList(Random.nextInt(friendIdList.length))
          var count:Int = 0
          while(!pubKeyMap.isDefinedAt(rndmfrndid_aes) || (frndsWithAccess.length>0 && frndsWithAccess.contains(rndmfrndid_aes))){
            count=count+1
            //          if(count == friendIdList.length){
            //            rndmfrndid_aes = frndsWithAccess(Random.nextInt(frndsWithAccess.length))
            //          }else{
            rndmfrndid_aes = friendIdList(Random.nextInt(friendIdList.length))
            //}
          }
          /*Send message only if it hasn't been already sent the same message before*/
          if(!frndsWithAccess.contains(rndmfrndid_aes)){

            frndsWithAccess += rndmfrndid_aes

            /*Inserting AES key encrypted with friend's public key*/
            var rndmfrndPubKey = pubKeyMap(rndmfrndid_aes)
            var pubKeyCipher_AES = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            pubKeyCipher_AES.init(Cipher.ENCRYPT_MODE,rndmfrndPubKey)
            var encryptedAesPublicKey = pubKeyCipher_AES.doFinal(aesKeyinString.getBytes("UTF-8"))
            var encryptedAesPublicKeyString = Base64.getEncoder.encodeToString(encryptedAesPublicKey)
            var aesEncWPub_F = rndmfrndid_aes+"!"+encryptedAesPublicKeyString
            aesEncPubList += aesEncWPub_F
          }
        }
        self ! post_to_own_wall(thisUserId,encryptedMessageString,initVectorString,aesEncPubList)
      }
    }

    def postToUserFunc(){
      if(!friendIdList.isEmpty){
        var ptf_postText=posts(Random.nextInt(distinctPosts))
        var ptf_cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        var ptf_aesKeyGenerator = KeyGenerator.getInstance("AES")
        ptf_aesKeyGenerator.init(128)
        var ptf_aesKey = ptf_aesKeyGenerator.generateKey()
        ptf_cipher.init(Cipher.ENCRYPT_MODE, ptf_aesKey, new IvParameterSpec(initVector))

        var ptf_aesKeyinString = Base64.getEncoder().encodeToString(ptf_aesKey.getEncoded)
        var ptf_encryptedMessage = ptf_cipher.doFinal(ptf_postText.getBytes("UTF-8"))
        var ptf_encryptedMessageString = Base64.getEncoder.encodeToString(ptf_encryptedMessage)
        var ptf_initVectorString = Base64.getEncoder.encodeToString(ptf_cipher.getIV)

        var ptf_aesEncPubList = new ListBuffer[String]()
        /*Inserting own public key-encrypted aes key*/
        var ptf_pubKeyCipher_AES = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        ptf_pubKeyCipher_AES.init(Cipher.ENCRYPT_MODE,pubKey)
        var ptf_encryptedAesPublicKey = ptf_pubKeyCipher_AES.doFinal(ptf_aesKeyinString.getBytes("UTF-8"))
        var ptf_encryptedAesPublicKeyString = Base64.getEncoder.encodeToString(ptf_encryptedAesPublicKey)
        var ptf_aesEncWPub = thisUserId+"!"+ptf_encryptedAesPublicKeyString
        ptf_aesEncPubList += ptf_aesEncWPub

        /*Selecting friend*/
        var ptf_rndm_frndid=friendIdList(Random.nextInt(friendIdList.length))
        while(!pubKeyMap.isDefinedAt(ptf_rndm_frndid)){
          ptf_rndm_frndid=friendIdList(Random.nextInt(friendIdList.length))
        }


        /*Inserting friend's public key-encrypted aes key*/
        var ptf_rndm_frnd_pubKey=pubKeyMap(ptf_rndm_frndid)
        ptf_pubKeyCipher_AES = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        ptf_pubKeyCipher_AES.init(Cipher.ENCRYPT_MODE,ptf_rndm_frnd_pubKey)
        ptf_encryptedAesPublicKey = ptf_pubKeyCipher_AES.doFinal(ptf_aesKeyinString.getBytes("UTF-8"))
        ptf_encryptedAesPublicKeyString = Base64.getEncoder.encodeToString(ptf_encryptedAesPublicKey)
        ptf_aesEncWPub = ptf_rndm_frndid+"!"+ptf_encryptedAesPublicKeyString
        ptf_aesEncPubList += ptf_aesEncWPub


        self ! post_to_friend(thisUserId,ptf_rndm_frndid,ptf_encryptedMessageString,ptf_initVectorString,ptf_aesEncPubList)
      }
    }

    def postToPageFunc(){
      if(!pageIdList.isEmpty){
        self ! post_to_page(thisUserId,pageIdList(Random.nextInt(pageIdList.length)),posts(Random.nextInt(distinctPosts)))
      }
    }
  }
}