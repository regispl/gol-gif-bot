package im.michalski.golgifbot.config


object Parser {
  val parser = new scopt.OptionParser[Config]("golgifbot") {
    head("GolGifBot", "0.1.0")

    opt[String]("reddit-username").required().action((x, c) =>
      c.copy(redditUsername = x))

    opt[String]("reddit-password").required().action((x, c) =>
      c.copy(redditPassword = x))

    opt[String]("reddit-client-id").required().action((x, c) =>
      c.copy(redditClientId = x))

    opt[String]("reddit-client-secret").required().action((x, c) =>
      c.copy(redditClientSecret = x))

    opt[String]("wykop-login").required().action((x, c) =>
      c.copy(wykopLogin = x))

    opt[String]("wykop-application-key").required().action((x, c) =>
      c.copy(wykopApplicationKey = x))

    opt[String]("wykop-secret").required().action((x, c) =>
      c.copy(wykopSecret = x))

    opt[String]("wykop-account-key").required().action((x, c) =>
      c.copy(wykopAccountKey = x))

    opt[String]("last-published-id").required().action((x, c) =>
      c.copy(lastPublishedId = x)).text("Only entries newer than this Reddit ID will be processed")

    opt[Unit]("verbose").action( (_, c) =>
      c.copy(verbose = true) ).text("Print debug and trace logs (TODO)")

    opt[Unit]("dry-run").action( (_, c) =>
      c.copy(dryRun = true) ).text("Run without publishing to Wykop.pl (TODO)")

    help("help").text("Prints this usage text")
  }
}
