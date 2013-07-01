package com.softwaremill.codebrag.service.followups

import com.softwaremill.codebrag.dao._
import org.bson.types.ObjectId
import com.softwaremill.codebrag.domain._
import pl.softwaremill.common.util.time.Clock
import scala.Some
import com.typesafe.scalalogging.slf4j.Logging

class FollowupService(followupDao: FollowupDAO, commitInfoDao: CommitInfoDAO, commitCommentDao: CommitCommentDAO, userDao: UserDAO)(implicit clock: Clock)
  extends Logging {

  def deleteUserFollowup(userId: ObjectId, followupId: ObjectId): Either[String, Unit] = {
    followupDao.findById(followupId) match {
      case Some(followup) => {
        if(followup.isOwner(userId)) {
          Right(followupDao.delete(followupId))
        } else {
          Left("User not allowed to delete followup")
        }
      }
      case None => Left("No such followup for user to remove")
    }
  }

  def generateFollowupsForComment(currentComment: Comment) {
    findCommitWithCommentsRelatedTo(currentComment) match {
      case (None, _) => throwException(s"Commit ${currentComment.commitId} not found. Cannot createOrUpdateExisting follow-ups for nonexisting commit")
      case (Some(commit), List()) => throwException(s"No stored comments for commit ${currentComment.commitId}. Cannot createOrUpdateExisting follow-ups for commit without comments")
      case (Some(commit), existingComments) => generateFollowUps(commit, existingComments, currentComment)
    }

    def throwException(message: String) = throw new RuntimeException(message)
  }


  private def generateFollowUps(commit: CommitInfo, existingComments: List[Comment], currentComment: Comment) {
    val followUpCreationDate = clock.currentDateTimeUTC()
    val lastCommenterName = commenterNameFor(currentComment)
    usersToGenerateFollowUpsFor(commit, existingComments, currentComment).foreach(userId => {
      followupDao.createOrUpdateExisting(Followup.forComment(currentComment.id, currentComment.authorId, userId, followUpCreationDate, lastCommenterName, currentComment.threadId))
    })
  }

  private def commenterNameFor(currentComment: Comment) = {

    userDao.findById(currentComment.authorId).map(_.name).getOrElse({
        logger.warn("Cannot find current commenter's user name - generating UNKNOWN name")
        "unknown"
    })
  }

  private def findCommitWithCommentsRelatedTo(comment: Comment): (Option[CommitInfo], List[Comment]) = {
    (commitInfoDao.findByCommitId(comment.commitId), commitCommentDao.findAllCommentsInThreadWith(comment))
  }

  def usersToGenerateFollowUpsFor(commit: CommitInfo, comments: List[Comment], currentComment: Comment): Set[ObjectId] = {

    def uniqueCommenters: Set[ObjectId] = {
      comments.map(_.authorId).toSet
    }

    def addCommitAuthor(users: Set[ObjectId]): Set[ObjectId] = {
      val authorIdOpt = userDao.findByUserNameOrEmail(commit.authorName, commit.authorEmail).map(_.id)
      authorIdOpt.map(users + _).getOrElse({
        logger.warn(s"Unknown commit author ${commit.authorName} (${commit.authorEmail}). No such user registered. Cannot generate follow-up.")
        users
      })
    }

    def withoutCurrentCommentAuthor(users: Set[ObjectId]): Set[ObjectId] = {
      users.filterNot(_.equals(currentComment.authorId))
    }

    withoutCurrentCommentAuthor(addCommitAuthor(uniqueCommenters))
  }

}
