package com.softwaremill.codebrag.dao.followup

import com.softwaremill.codebrag.dao.sql.SQLDatabase
import org.bson.types.ObjectId
import scala.slick.model.ForeignKeyAction

trait SQLFollowupSchema {
  val database: SQLDatabase

  import database.driver.simple._
  import database._

  protected case class SQLFollowup(
    id: ObjectId,
    receivingUserId: ObjectId,
    threadCommitId: ObjectId,
    threadFileName: Option[String],
    threadLineNumber: Option[Int],
    lastReactionId: ObjectId,
    lastReactionAuthor: ObjectId)

  protected class Followups(tag: Tag) extends Table[SQLFollowup](tag, "followups") {
    def id                  = column[ObjectId]("id", O.PrimaryKey)
    def receivingUserId     = column[ObjectId]("receiving_user_id")
    def threadCommitId      = column[ObjectId]("thread_commit_id")
    def threadFileName      = column[Option[String]]("thread_file_name")
    def threadLineNumber    = column[Option[Int]]("thread_line_number")
    def lastReactionId      = column[ObjectId]("last_reaction_id")
    def lastReactionAuthor  = column[ObjectId]("last_reaction_author")

    def * = (id, receivingUserId, threadCommitId, threadFileName, threadLineNumber, lastReactionId, lastReactionAuthor) <>
      (SQLFollowup.tupled, SQLFollowup.unapply)
  }

  protected val followups = TableQuery[Followups]

  protected class FollowupsReactions(tag: Tag) extends Table[(ObjectId, ObjectId)](tag, "followups_reactions") {
    def followupId = column[ObjectId]("followup_id")
    def reactionId = column[ObjectId]("reaction_id")

    def pk = primaryKey("FOLLOWUP_REACTIONS_PK", (followupId, reactionId))
    def followup = foreignKey("FOLLOWUP_REACTIONS_FK", followupId, followups)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    def * = (followupId, reactionId)
  }

  protected val followupsReactions = TableQuery[FollowupsReactions]
}
