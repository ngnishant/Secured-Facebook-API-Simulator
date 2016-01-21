package org.facebookAPIRest

import java.util.Base64
import java.net.URLDecoder
import java.security.{SecureRandom, KeyPairGenerator, PublicKey, KeyFactory}
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import akka.actor._
import org.json4s.native._
import org.json4s.native.Serialization._
import spray.json._
import scala.collection.mutable
import java.io._
import spray.routing.SimpleRoutingApp
import scala.collection.immutable.List
import org.json4s._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import spray.http.{HttpResponse, HttpEntity}
import spray.can.Http
import spray.can.server.Stats
import spray.http.MediaTypes._
import java.awt.Desktop
import spray.json._
import scala.collection.mutable.{ListBuffer, SynchronizedMap,ArrayStack}
import java.net.URLDecoder
import java.util.HashMap
import java.util.concurrent.TimeUnit
import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.{RoundRobinPool, RoundRobinRouter}
import akka.util.Timeout
import scala.collection.mutable
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import spray.routing.SimpleRoutingApp
import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import spray.can.Http
import spray.can.server.Stats
import play.api.libs.json.Json
import spray.json._
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import spray.json.DefaultJsonProtocol

/*Actor methods*/
case class create_user(userId:String,fname:String,lname:String,age:String,sex:String,location:String,occupation:String,interestedIn:String)
case class add_friend(friend1:String, friend2:String)
case class post_message(userId:String, message:String)
case class get_posts(userId:String)
case class get_friends(userId:String)
case class list_all_users()
case class create_page(userId:String, pageId:String,desc:String)
case class list_all_pages()
case class subscribe(userId:String,pageId:String)
case class postToPage(userId:String,pageId:String,message:String)
case class postToUser(userId:String,friendId:String,message:String)
case class viewPage(pageId:String)
case class viewProfile(userId:String)
case class getStats()
case class get_notifications(userId:String)
case class stopSystem()
case class finalStop()
case class wall_post(json_object:String)
case class postTo_friend (json_object:String)
case class get_wall_posts (userID:String, friendID:String)
case class stepOneAuth()
case class stepTwoAuth(stepTwoJson:String)
case class userPubKeySet(userPubKeyJson:String)

/*Data Classes*/
case class jsonPerson(var userId: String,var fname: String,var lname:String,var age: String,var sex: String,var location: String,var occupation: String)
case class jsonPersons(var list: Seq[jsonPerson])
case class basicInfo(var firstName:String,var lastName:String,var age:String,var sex:String,var location:String,var occupation:String,var interestedIn:String)
case class Page(var pageId:String,var ownerId:String,var desc:String,var posts:ListBuffer[String],var followers:ListBuffer[String])
case class viewPageclass(var pageId:String,var ownerName:String,var desc:String,var noOffollowers:String,var posts:Seq[String])
//case class User(var userId:String, var generalInfo: basicInfo, var posts:ListBuffer[String], var friends:List[User], var subpages:ListBuffer[String],var notifications:ArrayStack[String])
case class User(var userId:String, var generalInfo: basicInfo, var postObjectList:List[PostClass], var friends:List[User], var subpages:ListBuffer[String],var notifications:ArrayStack[String])
case class pageList(var pageId:String,var desc:String,var ownerId:String)
case class userList(var userId:String,var fname:String,var lname:String)
case class ListuserList(var list:Seq[userList])
case class pagesList(var pageId:String, var desc:String, var ownerName:String, var ownerId:String)
case class ListpagesList(var list:Seq[pagesList])
case class subPagesPV(var pageId:String,var description:String)
case class friendsPV(var userId:String,var Name:String)
case class ProfileView(var userId:String,var fname:String,var lname:String,var age: String,var sex: String,var location: String,var occupation: String,var subscribedPages:Seq[subPagesPV],var postsList:Seq[String],var friends:Seq[friendsPV])
case class userNotifications(var notificationsList:Seq[String])
case class PostClass(var msg:String,var iv:String,var pub_aes_keys:ListBuffer[String])
case class findPostsToReturn(var msg: String,var iv: String,var key: String)

object PersonJsonProtocol extends DefaultJsonProtocol {
  implicit val personJsonFormat=jsonFormat7(jsonPerson)
  implicit val personsJsonFormat=jsonFormat1(jsonPersons)
}

object userlistJsonProtocol extends DefaultJsonProtocol {
  implicit val userlistJsonFormat=jsonFormat3(userList)
  implicit val listuserListJsonFormat=jsonFormat1(ListuserList)
}

object pagelistJsonProtocol extends DefaultJsonProtocol {
  implicit val pagelistJsonFormat=jsonFormat4(pagesList)
  implicit val listpageListJsonFormat=jsonFormat1(ListpagesList)
}

object PageJsonProtocol extends DefaultJsonProtocol {
  implicit val pageJsonFormat=jsonFormat5(viewPageclass)
}

object profileViewProtocol extends DefaultJsonProtocol {
  implicit val pvSubPagesJsonFormat=jsonFormat2(subPagesPV)
  implicit val pvFriendsJsonFormat=jsonFormat2(friendsPV)
  implicit val pvJsonFormat=jsonFormat10(ProfileView)
}

object notificationJsonProtocol extends DefaultJsonProtocol {
  implicit val notificationJsonFormat=jsonFormat1(userNotifications)
}

object findPostsToReturnJsonProtocol extends DefaultJsonProtocol {
  implicit val findPostsToReturnJsonFormat=jsonFormat3(findPostsToReturn)
}

object FB_Server extends App with SimpleRoutingApp{
  import PersonJsonProtocol._
  import userlistJsonProtocol._
  import pagelistJsonProtocol._
  import PageJsonProtocol._
  import profileViewProtocol._
  import notificationJsonProtocol._
  import findPostsToReturnJsonProtocol._

  class ServerStats{
    var totalPosts=0
    var totalUsers=0
    var totalPages=0
  }

//  class findPostsToReturn(Msg:String, IV:String) {
//    var msg: String = Msg
//    var iv: String = IV
//    var key: String = ""
//  }

//  class postsToReturn(){
//    var listofPosts = ListBuffer[findPostsToReturn]()
//  }
//
//

  var userMap=new mutable.HashMap[String, User] () with SynchronizedMap[String, User]
  var pagesMap=new mutable.HashMap[String, Page]() with SynchronizedMap[String, Page]
  var pubKeyMap_server=new mutable.HashMap[String, PublicKey]() with SynchronizedMap[String, PublicKey]

  var s=new ServerStats()

  /*End of basic structure*/
  var keyGen_server = KeyPairGenerator.getInstance("RSA")
  keyGen_server.initialize(512)
  var keypair_server = keyGen_server.genKeyPair
  var pvtKey_server = keypair_server.getPrivate
  var pubKey_server = keypair_server.getPublic

  var sRNG_server = SecureRandom.getInstance("SHA1PRNG")
  sRNG_server.setSeed(sRNG_server.generateSeed(32))
  var sRNG_No = sRNG_server.nextInt()

  implicit val actorSystem=ActorSystem()
  import actorSystem.dispatcher
  implicit val timeout=Timeout(1.seconds)
  val serverActorsCount=Runtime.getRuntime().availableProcessors()*100
  val server_actor = actorSystem.actorOf(Props(new ServerActor()))//.withRouter(RoundRobinRouter(serverActorsCount)))
  val kill_actor = actorSystem.actorOf(Props(new KillActor()))//.withRouter(RoundRobinRouter(serverActorsCount)))



  //println(sRNG_No)

  class ServerActor extends Actor{
    //import context.system._
    private implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[String])))

    println("Server Ready")

    println(sRNG_No.toString)

    def receive = {
      //case "Hello"=> {println("Hello")}
      case stepOneAuth() => {
        //println("stepOneAuth")
        //println(sRNG_No.toString)
        sender ! (sRNG_No.toString)
      }
      case stepTwoAuth(stepTwoJson:String) => {
        val json_parsed = Json.parse(stepTwoJson)
        var userId: String = json_parsed.\("userId").as[String]
        var enc_sRNG: String = json_parsed.\("sRNGEnc").as[String]
        //val encrypted_post = new PostClass(json_parsed.\("Msg").as[String],json_parsed.\("IV").as[String],json_parsed.\("encrypted_keys").as[ListBuffer[String]])
        //println(stepTwoAuth)
        if(pubKeyMap_server.isDefinedAt(userId)){
          //println("stepTwoAuth-1")
          var user_PubKey = pubKeyMap_server(userId)

          var auth_pubKeyCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
          auth_pubKeyCipher.init(Cipher.DECRYPT_MODE,user_PubKey)
          var dec_sRNG = new String(auth_pubKeyCipher.doFinal(Base64.getDecoder.decode(enc_sRNG.map(c => if(c == ' ') '+' else c)))).toInt

          if(dec_sRNG == sRNG_No){
            //println("Authenticated")
            sender ! "Authenticated"
          }else{
            //println("NotAunthenticated")
            sender ! "NotAunthenticated"
          }
        }else{
          //println("UserPublicKeyNotSavedAtServer")
          sender ! "UserPublicKeyNotSavedAtServer"
        }
      }
      case userPubKeySet(userPubKeyJson:String) => {
        val json_parsed = Json.parse(userPubKeyJson)
        var userId: String = json_parsed.\("userId").as[String]
        var pubKeyString: String = json_parsed.\("publicKeyString").as[String]
        pubKeyMap_server.put(userId,KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder.decode(pubKeyString.map(c => if(c == ' ') '+' else c)))))
      }
      case create_user(userId:String,fname:String,lname:String,age:String,sex:String,location:String,occupation:String,interestedIn:String) => {
        if(!userMap.isDefinedAt(userId)){
          //println("In actor")
          var basic_new=new basicInfo(fname,lname,age,sex,location,occupation,interestedIn)
          //var user_new=new User(userId,basic_new,new ListBuffer[String](),List[User](),new ListBuffer[String](),new ArrayStack[String]())
          var user_new=new User(userId,basic_new,List[PostClass](),List[User](),new ListBuffer[String](),new ArrayStack[String]())
          userMap.put(userId,user_new)
          //println(userMap.get(userId))
          //println("User Created")
          sender ! "User Created."
        }
        else {
          //println("User with given UserID already exists!!")
          sender ! "User with given UserID already exists!!"
        }
      }
      case add_friend(friend1:String, friend2:String) => {
        if(userMap.isDefinedAt(friend1) && userMap.isDefinedAt(friend2)){
          if(!userMap(friend1).friends.contains(friend2)){
            var user1=userMap(friend1)
            var user2=userMap(friend2)
            user1.friends=user2 :: user1.friends
            user2.friends=user1 :: user2.friends
            userMap.put(friend1,user1)
            userMap.put(friend2,user2)
          }
        }else{
          //println("Friend to be added does not exist, yet")
        }
      }
//      case post_message(userId:String, message:String) => {
//        if(userMap.isDefinedAt(userId)){
//          var user=userMap(userId)
//          user.posts +=message// :: user.posts
//          userMap.put(userId, user)
//        }else{
//          //println("UserId for the post message call does not exist, yet")
//        }
//      }
//      case get_posts(userId: String) => {
//        if(userMap.isDefinedAt(userId)){
//          sender ! writePretty(userMap(userId).posts)
//        }else{
//          //println("UserId for the getPosts call does not exist, yet")
//          sender ! "UserId for the getPosts call does not exist, yet"
//        }
//      }
      case get_friends(userId:String) => {
        //sender ! writePretty(PersonProfile(userId,PersonBasicInfo(userMap.get(userId).friends)))
        if(userMap.isDefinedAt(userId)){
          var jsonPersonList=List[jsonPerson]()
          var userFriendsList=userMap(userId).friends
          for(friend <- userFriendsList){
            var userfriend=new jsonPerson(friend.userId,friend.generalInfo.firstName,friend.generalInfo.lastName,friend.generalInfo.age,friend.generalInfo.sex,friend.generalInfo.location,friend.generalInfo.occupation)
            jsonPersonList=userfriend :: jsonPersonList
          }
          sender ! writePretty(jsonPersons(jsonPersonList))//.toJson.prettyPrint
        }else{
          sender ! "UserId for the getFriends call does not exist, yet"
        }
        //var personslist=new jsonPersons(jsonPersonList)
      }
      case list_all_users() => {
        var listofallusers=List[userList]()
        for((key,value) <- userMap){
          var tempuser = value
          var tempuserforlist = new userList(tempuser.userId, tempuser.generalInfo.firstName, tempuser.generalInfo.lastName)
          listofallusers = tempuserforlist :: listofallusers
        }
        //var listofuserlist=new ListuserList(listofallusers)
        sender ! writePretty(ListuserList(listofallusers))//.toJson.prettyPrint
      }
      case create_page(userId:String,pageId:String,desc:String) => {
        if(userMap.isDefinedAt(userId)){
          //Adding page to User
          var user=userMap(userId)
          //println(user.subpages)
          //println(pageId)
          user.subpages += pageId// :: user.subpages
          userMap.put(userId,user)

          if(!pagesMap.isDefinedAt(pageId)){
            //Creating Page
            var page=new Page(pageId,userId,desc,new ListBuffer[String](),new ListBuffer[String]())
            page.followers += userId// :: page.followers
            pagesMap.put(pageId,page)
          }else{
            //println("Page with PageId passed already exists")
          }

        }else {
          //println("UserId for the createPage call does not exist, yet")
        }
      }
      case list_all_pages() => {
        var listofallpages=List[pagesList]()
        for((key,value) <- pagesMap){
          var temppage = value
          var temppageforlist = new pagesList(temppage.pageId,temppage.desc,userMap(temppage.ownerId).generalInfo.firstName+" "+userMap(temppage.ownerId).generalInfo.lastName,temppage.ownerId)
          listofallpages = temppageforlist :: listofallpages
        }
        sender ! writePretty(ListpagesList(listofallpages))
      }
      case subscribe(userId:String,pageId:String) => {
        if(userMap.isDefinedAt(userId)){
          var user=userMap(userId)
          user.subpages +=pageId// :: user.subpages
          userMap.put(userId,user)

          if(pagesMap.isDefinedAt(pageId)){
            var page=pagesMap(pageId)
            //println(page)
            page.followers += userId// :: page.followers
            pagesMap.put(pageId,page)
          }else{
            //println("PageId passed to the subscribeToPage call does not exist, yet")
          }
        }else{
          //println("UserId passed to the subscribeToPage call does not exist, yet")
        }
      }
      case postToPage(userId:String,pageId:String,message:String) => {
        if(userMap.isDefinedAt(userId)){
          if(pagesMap.isDefinedAt(pageId)){

            if(pagesMap(pageId).followers.contains(userId)){
              var page=pagesMap(pageId)
              page.posts += ((userMap(userId).generalInfo.firstName)+" "+(userMap(userId).generalInfo.lastName)+" posted : "+message) //:: page.posts
              pagesMap.put(pageId,page)
              sender ! "Message posted to Page's timeline."
            }else{
              sender ! "Message cannot be posted as the user has not subscribed to the Page."
            }

          }else{
            //println("PageId passed to the postToPage call does not exist, yet")
            sender ! "PageId passed to the postToPage call does not exist, yet"
          }
        }else{
          //println("UserId passed to postToPage call does not exist, yet")
          sender ! "UserId passed to postToPage call does not exist, yet"
        }
      }
//      case postToUser(userId:String,friendId:String,message:String) => {
//        //sender ! "Done"
//        if(userMap.isDefinedAt(userId) && userMap.isDefinedAt(friendId)){
//          if(userMap(userId).friends.contains(userMap(friendId))){
//            var friend=userMap(friendId)
//            friend.posts += ((userMap(userId).generalInfo.firstName)+" "+(userMap(userId).generalInfo.lastName)+" posted : "+message) //:: friend.posts
//            friend.notifications.push((userMap(userId).generalInfo.firstName)+" "+(userMap(userId).generalInfo.lastName)+" posted a message to your timeline.")
//            userMap.put(friendId,friend)
//
//            sender ! "Message posted to friend's timeline."
//          }else{
//            sender ! "Message cannot be posted as the users are not friends."
//          }
//        }else{
//          //println("FriendId passed to postToUser does not exist, yet")
//          sender ! "FriendId passed to postToUser does not exist, yet"
//        }
//      }
      case viewPage(pageId:String) => {
        if(pagesMap.isDefinedAt(pageId)){
          var page=pagesMap(pageId)
          //var viewpage=new viewPageclass(page.pageId,(userMap.get(page.ownerId).generalInfo.firstName)+" "+(userMap.get(page.ownerId).generalInfo.lastName),page.desc,page.followers.size.toString(),page.posts)
          //ender ! viewpage.toJson.prettyPrint
          sender ! writePretty(viewPageclass(page.pageId,(userMap(page.ownerId).generalInfo.firstName)+" "+(userMap(page.ownerId).generalInfo.lastName),page.desc,page.followers.size.toString(),page.posts))
        }else{
          //println("pageId passed to viewPage does not exist, yet")
          sender ! "pageId passed to viewPage does not exist, yet"
        }
      }
//      case viewProfile(userId:String) => {
//        if(userMap.isDefinedAt(userId)){
//          var user=userMap(userId)
//          var pageslist=List[subPagesPV]()
//          var friendslist=List[friendsPV]()
//
//          for(pageId <- user.subpages){
//            pageslist=new subPagesPV(pageId,pagesMap(pageId).desc) :: pageslist
//          }
//
//          for(friend <- user.friends){
//            friendslist=new friendsPV(friend.userId,userMap(friend.userId).generalInfo.firstName+" "+userMap(friend.userId).generalInfo.lastName) :: friendslist
//          }
//          sender ! writePretty (ProfileView(userId,userMap(userId).generalInfo.firstName,userMap(userId).generalInfo.lastName,userMap(userId).generalInfo.age,userMap(userId).generalInfo.sex,userMap(userId).generalInfo.location,userMap(userId).generalInfo.occupation,pageslist,userMap(userId).posts,friendslist))//.toJson.prettyPrint
//        }else{
//          //println("userId passed to viewProfile does not exist, yet")
//          sender ! "userId passed to viewProfile does not exist, yet"
//        }
//      }
      case getStats() => {
        sender ! ("The stats are : Total no. of users: "+s.totalUsers+" || Total no. of pages: "+s.totalPages+" || Total no. of posts: "+s.totalPosts)
      }
      case get_notifications(userId:String) => {
        //println("here too")
        if(userMap.isDefinedAt(userId)){
          if(userMap(userId).notifications.isEmpty){
            sender ! "There are no notifications for this user"
          }else{
            var notifications = userMap(userId).notifications.toList
            sender ! writePretty(userNotifications(notifications))
          }
        }else{
          sender ! "UserId passed to get_notifications does not exist, yet"
        }
      }
      case wall_post(json_object:String) => {
        val json_parsed = Json.parse(json_object)
        var userID: String = json_parsed.\("userID").as[String]
        val encrypted_post = new PostClass(json_parsed.\("Msg").as[String],json_parsed.\("IV").as[String],json_parsed.\("encrypted_keys").as[ListBuffer[String]])
        if (userMap.isDefinedAt(userID)){
          var user_obj = userMap(userID)
          user_obj.postObjectList = encrypted_post :: user_obj.postObjectList
          userMap.put(userID, user_obj)
          //stats_actor ! update_posts()
          sender ! (user_obj.generalInfo.firstName+" "+user_obj.generalInfo.lastName+ " posted status update")
        }
        else sender ! "UserID for the wall_post does not exist, yet"
      }

      case postTo_friend (json_object:String) => {
        val json_parsed = Json.parse(json_object)
        var userID: String = json_parsed.\("userID").as[String]
        var friendID: String = json_parsed.\("friendID").as[String]
        val encrypted_post = new PostClass(json_parsed.\("Msg").as[String],json_parsed.\("IV").as[String],json_parsed.\("encrypted_keys").as[ListBuffer[String]])
        if ((userMap.isDefinedAt(userID)) && (userMap.isDefinedAt(friendID))) {
          var user_obj1 = userMap(userID)
          var user_obj2 = userMap(friendID)
          if (user_obj2.friends.contains(user_obj1)) {  //Privacy Settings- Can post only on friend's wall
            user_obj2.postObjectList = encrypted_post :: user_obj2.postObjectList
            userMap.put(friendID,user_obj2)
            //stats_actor ! update_posts()
            sender ! (user_obj1.generalInfo.firstName+" "+user_obj1.generalInfo.lastName+" posted on the wall of "+user_obj2.generalInfo.firstName+" "+user_obj2.generalInfo.lastName)
          }
          else {
            sender ! (user_obj1.generalInfo.firstName+" "+user_obj1.generalInfo.lastName+" is not a friend of "+user_obj2.generalInfo.firstName+" "+user_obj2.generalInfo.lastName)
          }
        }
        else sender ! "userId passed in post_to_friend does not exist, yet"
      }

      case get_wall_posts(userID:String, friendID:String) => {
        if (userID.equalsIgnoreCase(friendID)) {
          if (userMap.isDefinedAt(userID)) {
            var user_obj1 = userMap(userID)
            var list_posts_to_return = new ListBuffer[findPostsToReturn]()
            //for (i <- 0 to (user_obj1.postObjectList.length-1)){
            for (postobject <- user_obj1.postObjectList){
              var count = 0
              var posts_to_return = new findPostsToReturn(postobject.msg, postobject.iv,"")
              var aes_keys_list = postobject.pub_aes_keys
              //for (j <- 0 to (aes_keys_list.length-1) ){ // foreach
              for(encAes_key <- aes_keys_list){
                var k = encAes_key.indexOf("!")
                if (encAes_key.substring(0,(k)).equalsIgnoreCase(userID)){
                  posts_to_return.key = encAes_key.substring(k+1,encAes_key.length)
                  count += 1
                }
              }
              if(count>0){
                list_posts_to_return += posts_to_return
              }
            }
            sender ! writePretty(list_posts_to_return)
          }
          else "UserId passed in get_wall_posts does not exist, yet"
        }
        else {
          if ((userMap.isDefinedAt(userID)) && (userMap.isDefinedAt(friendID))) {
            var user_obj1 = userMap(userID)
            var user_obj2 = userMap(friendID)
            if (user_obj2.friends.contains(user_obj1)) { //Privacy Settings- Can view only friend's wall
              var list_posts_to_return = new ListBuffer[findPostsToReturn]()
                //for (i <- 0 to (user_obj2.postObjectList.size-1)){
                for(postObject <- user_obj2.postObjectList){
                  var count = 0
                  var posts_to_return = new findPostsToReturn(postObject.msg, postObject.iv,"")
                  var aes_keys_list = postObject.pub_aes_keys
                  //for (j <- 0 to (aes_keys_list.size-1) ){
                  for(encAesKey <- aes_keys_list){
                    var k = encAesKey.indexOf("!")
                    if (encAesKey.substring(0,(k)).equalsIgnoreCase(userID)){
                      posts_to_return.key = encAesKey.substring(k+1,encAesKey.length)
                      count += 1
                    }
                  }
                  if(count>0){
                    list_posts_to_return += posts_to_return
                  }
                }
                sender ! writePretty(list_posts_to_return)
            }
            else{
              "The two userIds passed in get_wall_posts are not friends!!"
            }
          }
          else "UserId passed in get_wall_posts does not exist, yet"
        }
      }

      /*Stop*/
//      case stopSystem() => {
//        //println("here")
//        context.system.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess{
//          case s:Stats => {
//            val df = new java.text.SimpleDateFormat("HH:mm:ss")
//            df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
//            var data=HttpResponse(
//              entity = HttpEntity(`text/html`,
//                <html>
//                  <body>
//                    <h1>Facebook Server Stats</h1>
//                    <table>
//                      <tr><td>uptime:</td><td>{df.format(s.uptime.toMillis)}</td></tr>
//                      <tr><td>totalRequests:</td><td>{s.totalRequests}</td></tr>
//                      <tr><td>openRequests:</td><td>{s.openRequests}</td></tr>
//                      <tr><td>maxOpenRequests:</td><td>{s.maxOpenRequests}</td></tr>
//                      <tr><td>totalConnections:</td><td>{s.totalConnections}</td></tr>
//                      <tr><td>openConnections:</td><td>{s.openConnections}</td></tr>
//                      <tr><td>maxOpenConnections:</td><td>{s.maxOpenConnections}</td></tr>
//                      <tr><td>requestTimeouts:</td><td>{s.requestTimeouts}</td></tr>
//                    </table>
//                  </body>
//                </html>.toString()
//              )
//            ).entity.asString
//            val writer = new java.io.PrintWriter(new FileWriter("stats.html", false))
//            try writer.write(data + "\n") finally writer.close()
//            var statsFile = new File("stats.html")
//            Desktop.getDesktop().browse(statsFile.toURI())
//            //self ! finalStop()
//            //context.stop(self)
//            context.system.shutdown()
//          }
//        }
//      }
//      case finalStop() => {
//        //server_actor ! "kill"
//        context.stop(self)
//        context.system.shutdown()
//      }
//      case "kill" => {
//        context.stop(self)
//      }
    }
  }

  class KillActor extends Actor{
    def receive = {
      case stopSystem() => {
        //println("in kill")
        context.system.actorSelection("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess{
          case s:Stats => {
            val df = new java.text.SimpleDateFormat("HH:mm:ss")
            df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
            var data=HttpResponse(
              entity = HttpEntity(`text/html`,
                <html>
                  <body>
                    <h1>Facebook Server Stats</h1>
                    <table>
                      <tr><td>uptime:</td><td>{df.format(s.uptime.toMillis)}</td></tr>
                      <tr><td>totalRequests:</td><td>{s.totalRequests}</td></tr>
                      <tr><td>openRequests:</td><td>{s.openRequests}</td></tr>
                      <tr><td>maxOpenRequests:</td><td>{s.maxOpenRequests}</td></tr>
                      <tr><td>totalConnections:</td><td>{s.totalConnections}</td></tr>
                      <tr><td>openConnections:</td><td>{s.openConnections}</td></tr>
                      <tr><td>maxOpenConnections:</td><td>{s.maxOpenConnections}</td></tr>
                      <tr><td>requestTimeouts:</td><td>{s.requestTimeouts}</td></tr>
                    </table>
                  </body>
                </html>.toString()
              )
            ).entity.asString
            val writer = new java.io.PrintWriter(new FileWriter("stats.html", false))
            try writer.write(data + "\n") finally writer.close()
            var statsFile = new File("stats.html")
            Desktop.getDesktop().browse(statsFile.toURI())
            context.system.shutdown()
          }
        }
      }
    }
  }

  startServer(interface="localhost",port=8080){
    post{
      path("create"){
        //println("At Create")
        parameters("userId","fname","lname","age","sex","location","occupation","interestedIn"){
          (userId,fname,lname,age,sex,location,occupation,interestedIn) =>
            //println("Inside parameters")
            s.totalUsers+=1
            //pubKeyMap_server.put(userId,KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder.decode(pubKey.map(c => if(c == ' ') '+' else c)))))
            //KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes))
            complete{
              //"User Created!"
              (server_actor ? create_user(userId,fname,lname,age,sex,location,occupation,interestedIn)).mapTo[String]
            }
        }
      }
    }~
    post{
      path("userPubKeyPath"){
        parameters("userPubKeyJson"){
          (userPubKeyJson) => {
            server_actor ! userPubKeySet(URLDecoder.decode(userPubKeyJson,"UTF-8"))
          }
          complete{
            "User's Public Key Saved!"
          }
        }
      }
    }~
    post{
      path("initClient"){
        //println("initClient received!!")
        complete{
          (server_actor ? stepOneAuth()).mapTo[String].map(s => s"$s")
        }
      }
    }~
    post{
      path("initClientStepTwo"){
        parameters("stepTwoJson"){
          (stepTwoJson) => {
            complete{
              (server_actor ? stepTwoAuth(URLDecoder.decode(stepTwoJson,"UTF-8"))).mapTo[String].map(s => s"$s")
            }
          }
        }
      }
    }~
    post{
      path("addfriend"){
        parameters("friend1","friend2"){
          (friend1,friend2) => server_actor ! add_friend(friend1,friend2)
          complete{
            "The two users are now friends!"
          }
        }
      }
    }~
//    post{
//      path("post"){
//        parameters("userId","message"){
//          (userId,message) => server_actor ! post_message(userId,message)
//            s.totalPosts+=1
//          complete{
//            "Message posted to user's timeline."
//          }
//        }
//      }
//    }~
//    get{
//      path("getposts"){
//        parameters("userId"){
//          (userId) => complete{
//            (server_actor ? get_posts(userId)).mapTo[String].map(s => s"$s")
//          }
//        }
//      }
//    }~
    get{
      path("getfriends"){
        parameters("userId"){
          (userId) => complete{
            (server_actor ? get_friends(userId)).mapTo[String].map(s => s"$s")
          }
        }
      }
    }~
    get{
      path("listUsers"){
        complete {
          (server_actor ? list_all_users()).mapTo[String].map(s => s"$s")
        }
      }
    }~
    post{
      path("createPage"){
        parameters("userId","pageId","desc"){
          (userId,pageId,desc) => server_actor ! create_page(userId,pageId,desc)
            s.totalPages+=1
          complete{
            "Page created!"
          }
        }
      }
    }~
    get{
      path("listPages"){
        complete{
          (server_actor ? list_all_pages()).mapTo[String].map(s => s"$s" )
        }
      }
    }~
    post{
      path("subscribeToPage"){
        parameters("userId","pageId"){
          (userId,pageId) => server_actor ! subscribe(userId,pageId)
          complete{
            "Page subscribed by the user."
          }
        }
      }
    }~
    post{
      path("postToPage"){
        parameters("userId","pageId","message"){
          (userId,pageId,message) => {
            s.totalPosts+=1
            complete{
              (server_actor ? postToPage(userId,pageId,message)).mapTo[String].map(s => s"$s")
            }
          }
        }
      }
    }~
//    post{
//      path("postToUser"){
//        parameters("userId","friendId","message"){
//          (userId,friendId,message) => {
//            s.totalPosts+=1
//            complete{
//              (server_actor ? postToUser(userId,friendId,message)).mapTo[String]
//            }
//          }
//        }
//      }
//    }~
    get{
      path("viewPage"){
        parameters("pageId"){
          (pageId) => {
            complete{
              (server_actor ? viewPage(pageId)).mapTo[String].map(s => s"$s")
            }
          }
        }
      }
    }~
//    get{
//      path("viewProfile"){
//        parameters("userId"){
//          (userId) => {
//            complete{
//              (server_actor ? viewProfile(userId)).mapTo[String].map(s => s"$s")
//            }
//          }
//        }
//      }
//    }~
    get{
      path("getStats"){
        complete{
          (server_actor ? getStats()).mapTo[String].map(s => s"$s")
        }
      }
    }~
    get{
      path("getNotifications"){
        parameters("userId"){
          (userId) => {
            complete {
              (server_actor ? get_notifications(userId)).mapTo[String].map(s => s"$s")
            }
          }
        }
      }
    }~
    post{
      path("stopServer"){
        complete{
          //println("Kill received!")
          kill_actor ! stopSystem()
          "System Stopped"
        }
      }
    }~
    post {
      path("post_msg") {
        parameter("post_msg_json"){ json_object =>
          s.totalPosts+=1
          complete {
            (server_actor ? wall_post(URLDecoder.decode(json_object,"UTF-8"))).mapTo[String].map(s => s"$s" )
          }
        }
      }
    }~
    post {
      path("post_to_friend") {
        parameter("post_to_friend_json"){ json_object =>
          s.totalPosts+=1
          complete {
            (server_actor ? postTo_friend(URLDecoder.decode(json_object,"UTF-8"))).mapTo[String].map(s => s"$s")
          }
        }
      }
    }~
    get {
      path("get_wall_posts") {
        parameter("userid","friendid"){ (userID, friendID) =>
          complete {
            (server_actor ? get_wall_posts(userID.toString, friendID.toString)).mapTo[String].map(s => s"$s")
          }
        }
      }
    }
  }
}