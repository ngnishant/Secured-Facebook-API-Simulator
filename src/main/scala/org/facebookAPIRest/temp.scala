package server

import java.io.FileInputStream
import java.security._
import java.util.Base64
import javax.crypto.{KeyGenerator, Cipher}
import javax.crypto.spec.IvParameterSpec

import com.sun.xml.internal.fastinfoset.algorithm.BASE64EncodingAlgorithm

//import org.apache.commons.codec.binary.Base64

import org.facebookAPIRest.PersonJsonProtocol._
import spray.json.DefaultJsonProtocol

//import api.{Test, Post}

import scala.collection.mutable


/**
  * Created by hsitas444 on 11/14/2015.
**/


object JsonTester extends App {



  import spray.json._
  //import api.JsonFormatter._
  //Some
  case class Post(var i:Int,var a:String,var b:String,var c:String)
  case class Test(var posts:Seq[Post])


  object PostsJsonProtocol extends DefaultJsonProtocol {
    implicit val postJsonFormat=jsonFormat4(Post)
    implicit val testJsonFormat=jsonFormat1(Test)
  }

  val post = Post(1, "123123", "desc", "link")
  val post1 =Post(2, "123123", "desc", "link")

//  val test = new Test(mutable.ListBuffer[Post]())
//  test.posts = post :: test.posts
//  println(test.toJson.compactPrint)
//  test.posts += post1

  var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
  //println(cipher.getBlockSize)
  //println(cipher.getAlgorithm)



  val sRNG = SecureRandom.getInstance("SHA1PRNG")
  sRNG.setSeed(sRNG.generateSeed(32))
  var initV = new Array[Byte](16)
  //println(initV)
  //val secretKeySpecification =
    sRNG.nextBytes(initV)
  //println("initIV : "+initV(0))
  //println(secretKeySpecification.toString)
  val test=Base64.getEncoder.encodeToString(initV)
  //println(Base64.getEncoder.encodeToString(initV))
  //println(Base64.getDecoder.decode(test))
  //  sRNG.nextBytes(initV)
  //  println(Base64.getEncoder.encodeToString(initV))
  //  sRNG.nextBytes(initV)
  //  println(Base64.getEncoder.encodeToString(initV))

  val ks = KeyStore.getInstance("jceks")

  // get user password and file input stream
  val storepass = "storepass".toCharArray
  val keypass = "keypass".toCharArray

  try {
    val generator = KeyGenerator.getInstance("AES")
    generator.init(128)
        //val key = ks.getKey("k1", keypass)
    var key = generator.generateKey()
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(initV))

//    var base64encodedstring=Base64.getEncoder.encodeToString(cipher.getIV)
//    println("base64 IV encoded to string : "+base64encodedstring)
//    println("base64 IV : "+Base64.getDecoder.decode(base64encodedstring)(0))

    var encryptedMessageInBytes = cipher.doFinal("Lor emfdhyf igvkgigigilgligflfliyflyfjlfhjfjhgiltlg".getBytes("UTF-8"))
    //println(encryptedMessageInBytes)
    //println(encryptedMessageInBytes.length)
    //println(Base64.getEncoder.encodeToString(encryptedMessageInBytes))
    //println(Base64.getEncoder.encodeToString(cipher.getIV))
    //println(Base64.getEncoder.encodeToString(encryptedMessageInBytes).length)
    //println(Base64.getEncoder.encodeToString(cipher.getIV).length)

//    key = generator.generateKey()
//    sRNG.nextBytes(initV)
//    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(initV))
//    encryptedMessageInBytes = cipher.doFinal("Lorem".getBytes("UTF-8"))
//    println(Base64.getEncoder.encodeToString(encryptedMessageInBytes))


    cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(initV))

    var decBytes = cipher.doFinal(encryptedMessageInBytes)
    //println("decrypted aes "+new String(decBytes))



    //PKC

    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(1024)
    var keypair = keyGen.genKeyPair
    val prvtKey = keypair.getPrivate
    val pubKey = keypair.getPublic
    //println(pubKey)
    if(prvtKey.isInstanceOf[PrivateKey]){
      //println("Key is private")
    }

    var pckCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    //    pckCipher.init(Cipher.ENCRYPT_MODE, prvtKey)
    pckCipher.init(Cipher.ENCRYPT_MODE, pubKey)

    encryptedMessageInBytes = pckCipher.doFinal("Lorem1".getBytes("UTF-8"))
    //println(encryptedMessageInBytes.length +" "+Base64.getEncoder.encodeToString(encryptedMessageInBytes)+"\n")
    //    pckCipher.init(Cipher.DECRYPT_MODE, pubKey)
    pckCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    pckCipher.init(Cipher.DECRYPT_MODE, prvtKey)

    decBytes = pckCipher.doFinal(encryptedMessageInBytes)
    //println(new String(decBytes))

    val singer =  Signature.getInstance("SHA256withRSA")
    singer.initSign(prvtKey)
    //singer.initSign(prvtKey.asInstanceOf[PrivateKey])
    singer.update("Lorem1".getBytes("UTF-8"))

    val singedBytes = singer.sign()
    //var sign=Base64.getEncoder.encodeToString(singedBytes)
    //println("Singed Bytes: "+ Base64.getEncoder.encodeToString(singedBytes))
    //println(Base64.getDecoder.decode(sign))
    val verifer =  Signature.getInstance("SHA256withRSA")
    verifer.initVerify(pubKey)
    verifer.update("Lorem1".getBytes("UTF-8"))
    val verifies = verifer.verify(singedBytes)



    //System.out.println("signature verifies: " + verifies);

    //val teststring = "Nishant".toByte
    //println(Base64.getEncoder.encodeToString(teststring))

    //var wallPostGetCipher_RSA.init(Cipher.DECRYPT_MODE, pvtKey)

    var s1 = new String("106!asdasda")
    var s2 = new String("10!asdadsa")

    println(s1.indexOf('!'))
//    println((s1.indexOf('!')-1))
//    println(s1.substring(0,(s1.indexOf('!')-1)))
    println(s1.substring(0,s1.indexOf('!')))
    println(s1.substring(s1.indexOf('!')+1,s1.length))

//    println(s2.indexOf('!'))
//    println(s2.substring(0,(s2.indexOf('!')-1)))





  }
  catch{
    case ex:Throwable=> println(ex)
  }






  //
  //  val date = JsonParser(post.toJson.prettyPrint)
  //  println(date.convertTo[Post].post_description)
  //  JsonParser(date.toJson.prettyPrint)

  //  println(date.toJson)
  //
  //  var date = Some(DateTime.now)
  //  val ah = AuthHeader(123,"12")
  //  println(date.toJson)
  //
  //  date = JsonParser(date.toJson.prettyPrint).convertTo[DateTime]
  //  println(date.toJson)

  //
  //  val friends:mutable.Map[Long, String] = new mutable.HashMap[Long, String]()
  //  for (i <- 0L to 1000000L ){
  //    if(i % 100000 == 0)
  //      println("i : " + i);
  //    friends += (i-> Random.alphanumeric.take(32).mkString)
  //  }
  //  println("build complete")
  //
  //  val randomInts = new ListBuffer[Long] ()
  //
  //  for (i <- 0L to 1000000L ){
  //    randomInts += Random.nextInt(10000000);
  //  }
  //  val start = System.currentTimeMillis()
  //  for (i <- 0L until 1000000L ){
  //    val x = friends.get(i)
  //  }
  //
  //
  //  val end = System.currentTimeMillis()
  //
  //  println("Time taken : " + (end - start)/1000.0)

  //val color = json.convertTo[Color]



//  implicit def muSetFormat[T :JsonFormat] = viaSeq[mutable.Set[T], T](seq => mutable.Set(seq :_*))
//
//  override implicit def mapFormat[K :JsonFormat, V :JsonFormat] = new RootJsonFormat[Map[K, V]] {
//    def write(m: Map[K, V]) = JsObject {
//      m.map { field =>
//        field._1.toJson match {
//          case JsString(x) => x -> field._2.toJson
//          case JsNumber(x) => x.toString() -> field._2.toJson
//          case x => throw new SerializationException("Map key must be formatted as JsString, not '" + x + "'")
//        }
//      }
//    }
//    def read(value: JsValue) = value match {
//      case x: JsObject => x.fields.map { field =>{
//        if(field._1.matches("[-+]?\\d+(\\.\\d+)?"))
//          (JsNumber(field._1).convertTo[K], field._2.convertTo[V])
//        else (JsString(field._1).convertTo[K], field._2.convertTo[V])
//      }
//      } (collection.breakOut)
//      case x => deserializationError("Expected Map as JsObject, but got " + x)
//    }
//  }

}