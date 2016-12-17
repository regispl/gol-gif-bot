package im.michalski.golgifbot.config


object Parser {
  val parser = new scopt.OptionParser[Config]("golgifbot") {
    head("GolGifBot", "0.1.0")

    opt[String]("reddit-username").required().action((x, c) =>
      c.copy(redditUsername = x)).text("Reddit account username")

    opt[String]("reddit-password").required().action((x, c) =>
      c.copy(redditPassword = x)).text("Reddit account password")

    opt[String]("reddit-client-id").required().action((x, c) =>
      c.copy(redditClientId = x)).text("Reddit application client ID")

    opt[String]("reddit-client-secret").required().action((x, c) =>
      c.copy(redditClientSecret = x)).text("Reddit application client secret")

    opt[String]("wykop-login").required().action((x, c) =>
      c.copy(wykopLogin = x)).text("Wykop account login")

    opt[String]("wykop-application-key").required().action((x, c) =>
      c.copy(wykopApplicationKey = x)).text("Wykop application key")

    opt[String]("wykop-secret").required().action((x, c) =>
      c.copy(wykopSecret = x)).text("Wykop application secret")

    opt[String]("wykop-account-key").required().action((x, c) =>
      c.copy(wykopAccountKey = x)).text("Wykop account key")

    opt[String]("last-published-id").optional().action((x, c) =>
      c.copy(lastPublishedId = Some(x))).text("Only entries newer than this Reddit ID will be processed")

    opt[Unit]("verbose").hidden().action( (_, c) =>
      c.copy(verbose = true) ).text("Print debug and trace logs (TODO)")

    opt[Unit]("dry-run").action( (_, c) =>
      c.copy(dryRun = true) ).text("Run without publishing to Wykop.pl")

    help("help").text("Prints this usage text")
  }
}
