package im.michalski.golgifbot.models

import io.circe.Decoder
import io.circe.generic.semiauto._


object JsonOps {
  implicit val accessTokenDecoder: Decoder[AccessToken] = deriveDecoder[AccessToken]
}
