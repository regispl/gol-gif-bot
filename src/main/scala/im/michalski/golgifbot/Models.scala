package im.michalski.golgifbot

case class Error(message: String)

case class AccessToken(
  access_token: String,
  expires_in: Int,
  scope: String,
  token_type: String
)
