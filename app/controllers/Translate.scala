package controllers

import play.api.libs.json._
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.ws.WS
import play.api.cache.Cache
import play.api.Play.current
import scala.concurrent.duration.Duration
import scala.xml.XML
import ExecutionContext.Implicits.global
import play.api.libs.concurrent.Akka


object Translate {

  def translate(from: String, to: String, text: String): Future[String] = {
    //TODO 例外処理
    if(text=="" || to=="" || from==""){
      return Akka.future {"(何か入力してください)"}
    }
    val accessToken = Await.result(getAccessToken(), Duration.Inf)
    //翻訳結果は適当に長時間キャッシュする
    //Cache.getOrElse[Future[String]](from + to + Utility.md5Sum(text), 60 * 60 * 24) {
      val future = WS.url("http://api.microsofttranslator.com/V2/Http.svc/Translate")
        .withHeaders(("Authorization", "Bearer " + accessToken))
        .withQueryString(("from", from), ("to", to), ("text", text))
        .get()
      future.map {
        response =>
          XML.loadString(response.body).text
      } recover {
        //TODO 例外処理
        case e: Exception => println("errr: " + e)
          ""
      }
    //}
  }

  private def getAccessToken(): Future[String] = {
    //AccessToken有効時間は10分あるので、10分ほどキャッシュする
    Cache.getOrElse[Future[String]]("TranslationAccessToken", 60 * 10 - 30) {
      val future = WS.url("https://datamarket.accesscontrol.windows.net/v2/OAuth2-13").post(Map(
        "grant_type" -> Seq("client_credentials"),
        "client_id" -> Seq("translate_trend"),
        "client_secret" -> Seq(Conf.translateClientSecret),
        "scope" -> Seq("http://api.microsofttranslator.com")
      ))
      future.map {
        response =>
          (Json.parse(response.body) \ "access_token").as[String]
      }
    }
  }

  /**
   * 2013.05.24のリストに準拠
   * @param from
   * @param to
   * @param text
   * @return
   */
  def isTranslate(from: String, to: String, text:String): Boolean = {
    val LanguageCodes = List("ar", "bg", "ca", "zh-CHS", "zh-CHT", "cs", "da", "nl", "en", "et", "fa", "fi", "fr", "de", "el", "ht", "he", "hi", "hu", "id", "it", "ja", "ko", "lv", "lt", "ms", "mww", "no", "pl", "pt", "ro", "ru", "sk", "sl", "es", "sv", "th", "tr", "uk", "ur", "vi")
    if (from != to && LanguageCodes.contains(from) && LanguageCodes.contains(to)) {
      true
    } else {
      false
    }
  }

  /*
   * md5を生成する
   */
  def md5Sum(message: String): String = {
    import java.security.MessageDigest
    val digestedBytes = MessageDigest.getInstance("MD5").digest(message.getBytes)
    digestedBytes.map("%02x".format(_)).mkString
  }
}

