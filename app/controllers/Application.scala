package controllers

import play.api.mvc._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Application extends Controller {

  //翻訳する
  def translate(text: String, from: String, to: String) = Action {
    Async {
      Translate.translate(from, to, text).map {
        result =>
          Ok(views.html.translate(result))
      }
    }
  }

  //翻訳して翻訳して元の言語に戻す
  def index(text: String, from: String, to: String) = Action {
    Async {
      Translate.translate(from, to, text).flatMap {
        translated =>
          Translate.translate(to, from, translated).map {
            reTranslated =>
              Ok(views.html.index(text, translated, reTranslated))
          }
      }
    }
  }

}
