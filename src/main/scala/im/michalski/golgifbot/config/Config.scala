package im.michalski.golgifbot.config


// TODO: Make lastPublishedId an option
case class Config(redditUsername: String = "", redditPassword: String = "", redditClientId: String = "",
                  redditClientSecret: String = "", wykopLogin: String = "", wykopApplicationKey: String = "",
                  wykopSecret: String = "", wykopAccountKey: String = "", lastPublishedId: String = "")
