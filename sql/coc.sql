USE [master]
GO
/****** Object:  Database [COC]    Script Date: 2015/07/05 03:28:34 PM ******/
CREATE DATABASE [COC]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'COC', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL11.MSSQLSERVER\MSSQL\DATA\COC.mdf' , SIZE = 5120KB , MAXSIZE = UNLIMITED, FILEGROWTH = 1024KB )
 LOG ON 
( NAME = N'COC_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL11.MSSQLSERVER\MSSQL\DATA\COC_log.ldf' , SIZE = 3136KB , MAXSIZE = 2048GB , FILEGROWTH = 10%)
GO
ALTER DATABASE [COC] SET COMPATIBILITY_LEVEL = 110
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [COC].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [COC] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [COC] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [COC] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [COC] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [COC] SET ARITHABORT OFF 
GO
ALTER DATABASE [COC] SET AUTO_CLOSE OFF 
GO
ALTER DATABASE [COC] SET AUTO_CREATE_STATISTICS ON 
GO
ALTER DATABASE [COC] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [COC] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [COC] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [COC] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [COC] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [COC] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [COC] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [COC] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [COC] SET  DISABLE_BROKER 
GO
ALTER DATABASE [COC] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [COC] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [COC] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [COC] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [COC] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [COC] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [COC] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [COC] SET RECOVERY FULL 
GO
ALTER DATABASE [COC] SET  MULTI_USER 
GO
ALTER DATABASE [COC] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [COC] SET DB_CHAINING OFF 
GO
ALTER DATABASE [COC] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [COC] SET TARGET_RECOVERY_TIME = 0 SECONDS 
GO
USE [COC]
GO
/****** Object:  StoredProcedure [dbo].[GetOurParticipants]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[GetOurParticipants]
	@warID int

AS
BEGIN

	SET NOCOUNT ON;

	SELECT     
		OurParticipantID, dbo.OurParticipant.PlayerID, dbo.Player.GameName
	FROM         
		dbo.OurParticipant INNER JOIN
        dbo.Player ON dbo.OurParticipant.PlayerID = dbo.Player.PlayerID
	WHERE     
		(dbo.OurParticipant.WarID = @warID)
END

GO
/****** Object:  UserDefinedFunction [dbo].[CompletedFirstAttack]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[CompletedFirstAttack]
(
	@warID INT
	,@ourRank INT
)
RETURNS BIT
AS
BEGIN
	DECLARE @result BIT;
	DECLARE @i int = -1;
	SET @result = (SELECT     COUNT(*) AS counter
		FROM dbo.Attack INNER JOIN
			dbo.OurParticipant ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID
		WHERE (dbo.Attack.OurAttack = 1) AND (NOT (dbo.Attack.StarsTaken IS NULL)) AND dbo.Attack.WarID = @warID
		GROUP BY dbo.OurParticipant.Rank
		HAVING (dbo.OurParticipant.Rank = @ourRank));
	SET @i = @result;
	IF (@result IS NULL)
	BEGIN
		SET @result = 0
	END
	ELSE
	BEGIN
		SET @result = 1
	END
	RETURN @result
END

GO
/****** Object:  UserDefinedFunction [dbo].[CompletedSecondAttack]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[CompletedSecondAttack] 
(
	@warID INT
	,@opponent INT
)
RETURNS BIT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @result BIT
	SET @result = (SELECT     
		COUNT(*) AS counter
	FROM         
		dbo.Attack INNER JOIN
        dbo.OurParticipant ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID
	WHERE     
		(dbo.Attack.OurAttack = 1) AND (dbo.Attack.FirstAttack = 0) AND (NOT (dbo.Attack.StarsTaken IS NULL)) AND dbo.Attack.WarID = @warID
	GROUP BY dbo.OurParticipant.Rank
	HAVING      
		(dbo.OurParticipant.Rank = @opponent))
	IF (@result IS NULL)
	BEGIN
		SET @result = 0
	END
	ELSE
	BEGIN
		SET @result = 1
	END
	RETURN @Result
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetAvailableNeighbour]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetAvailableNeighbour] 
(
	@WarID INT
	,@OwnRank INT
)
RETURNS INT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Result INT
	DECLARE @counter INT
		,@lowerNeighbour INT = 0
		,@higherNeighbour INT = 0
		,@numberOfStars INT

	-- Is the higher neighbour available?
	IF (@OwnRank > 1) -- This is the highest rank
	BEGIN
		SET @numberOfStars = (SELECT [dbo].[GetMaxStars](@WarID, @OwnRank - 1))
		IF (@numberOfStars < 2) --Is the next higher opponent available
			SET @higherNeighbour = @OwnRank - 1
	END

	-- Is the lower neighbour available?
	IF (@OwnRank < [dbo].[GetNumberOfParticipants](@WarID)) -- This is the lowest rank
	BEGIN
		SET @numberOfStars = (SELECT [dbo].[GetMaxStars](@WarID, @OwnRank + 1))
		IF (@numberOfStars < 2)
			SET @lowerNeighbour = @OwnRank + 1
	END

	-- Check the lower rank first
	IF (@lowerNeighbour > 0)
	BEGIN
		-- Check if this opponent has already been attacked by our rank
		SELECT @numberOfStars = (SELECT COUNT(*) FROM dbo.View_OurAttackedOpponents WHERE (WarID = @WarID) AND (OurRank = @OwnRank) AND (TheirRank = @lowerNeighbour))
		IF (@numberOfStars > 0)
			SET @Result = 0
		ELSE
			SET @Result = @lowerNeighbour
	END
	ELSE 
	BEGIN
		SELECT @numberOfStars = (SELECT COUNT(*) FROM dbo.View_OurAttackedOpponents WHERE (WarID = @WarID) AND (OurRank = @OwnRank) AND (TheirRank = @higherNeighbour))
		IF (@numberOfStars > 0)
			SET @Result = 0
		ELSE
			SET @Result = @higherNeighbour
	END
END_OF_FUNCTION:
	RETURN @Result
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetClosestAvailabledAttackOpponent]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetClosestAvailabledAttackOpponent] 
(
	@warID int, 
	@ownRank int
)
RETURNS int
AS
BEGIN
	DECLARE @rankToAttack int
	DECLARE @alreadyAttacked bit = 0
		,@defeated bit = 0
		,@foundMatch bit = 0
		,@maxStars int = 0
		,@attacked BIT = 0
		,@result BIT = 0

		-- Did the player already attacked twice
		SET @attacked = [dbo].[GetNumberOfAttacks](@warID, @ownRank)
		IF (@attacked = 2)
		BEGIN
			SET @rankToAttack = 0
			GOTO END_OF_PROCEDURE
		END

		-- Lets see if their is a next lower rank opponent available for him to attack
		SET @rankToAttack =  (SELECT [dbo].GetAvailableNeighbour(@warID, @ownRank))
		IF (@rankToAttack > 0 AND @rankToAttack >  @ownRank) --There is neighbours available that has not been attacked yet so we go for the next Lowest opponent
		BEGIN
			GOTO END_OF_PROCEDURE --We are done
		END

		SET @alreadyAttacked = (SELECT dbo.OwnDirectOpponentAttacked(@warID, @ownRank));
		IF (@alreadyAttacked = 0) --This player did not yet attack his direct opponent
		BEGIN
			SET @maxStars = (SELECT dbo.GetMaxStars(@warID, @ownRank)) --Check if anybody else already defeated his direct opponent
			IF (@maxStars >= 2)
				SET @defeated = 1;
		END

		IF (@alreadyAttacked = 0 AND @defeated = 0) --This player's direct opponent is available for him to attack
		BEGIN
			SET @rankToAttack = @ownRank
		END

		IF ((@alreadyAttacked = 0 AND @defeated = 1) OR -- He did not attacked his own opponent but the opponent was already defeated
			(@alreadyAttacked = 1 AND @defeated = 0))  -- OR he already attacked his own direct opponent
		BEGIN
			-- Get the next lowest available opponent where the possability is slim for a first attacker to defeat him
			-- See if there is any of this player's opponent's neighbours available for a first attack
			SET @rankToAttack = (SELECT dbo.GetNextLowest(@warID, @ownRank)) -- Get the next highest available opponent
			IF (@rankToAttack = 0) -- There is no lower rank available to attack
			BEGIN
				SET @rankToAttack = (SELECT dbo.GetNextHighest(@warID, @ownRank))  --So we go for the next highest opponent
				GOTO END_OF_PROCEDURE --We are done
			END
			SET @rankToAttack = (SELECT dbo.GetNextLowest(@warID, @ownRank))  --So we go for the next lowest opponent
			IF (@rankToAttack = 0) -- There is no lower rank available to attack
			BEGIN
				SET @rankToAttack = (SELECT dbo.GetNexthighest(@warID, @ownRank)) -- Get the next highest available opponent
			END
		END
END_OF_PROCEDURE:
RETURN @rankToAttack
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetClosestFirstAttackOpponent]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date, ,>
-- Description:	<Description, ,>
-- =============================================
CREATE FUNCTION [dbo].[GetClosestFirstAttackOpponent]
(
	@warID int, 
	@ownRank int
)
RETURNS int
AS
BEGIN
	DECLARE @alreadyAttacked bit = 0
		,@defeated bit = 0
		,@foundMatch bit = 0
		,@maxStars int = 0
		,@attack BIT = 0
		,@rankToAttack int

		SET @attack = (SELECT [dbo].[CompletedFirstAttack](@warid,@ownRank))
		IF (@attack = 1) -- This player already did his first attack
		BEGIN
			SET @rankToAttack = 0
			GOTO END_OF_PROCEDURE		
		END
		SET @alreadyAttacked = (SELECT [dbo].[OwnDirectOpponentAttacked](@warID, @ownRank));
		IF (@alreadyAttacked = 0) --This player did not yet attack his direct opponent
		BEGIN
		SET @maxStars = (SELECT [dbo].[GetMaxStars](@warID, @ownRank)); --Check if anybody else already defeated his direct opponent
			IF (@maxStars >= 2)
				SET @defeated = 1;
		END

		IF (@alreadyAttacked = 0 AND @defeated = 0) --This player's direct opponent is available for him to attack
		BEGIN
			SET @rankToAttack = @ownRank
		END

		IF ((@alreadyAttacked = 0 AND @defeated = 1) OR -- He did not attacked his own opponent but the opponent was already defeated
			(@alreadyAttacked = 1 AND @defeated = 0))  -- Already attacked own direct opponent
		BEGIN
			SET @rankToAttack = (SELECT dbo.GetNextLowest(@warID, @ownRank)) -- Get the next lowest available opponent
			IF (@rankToAttack = 0) -- There is no lower rank available to attack
				SET @rankToAttack = (SELECT dbo.GetNexthighest(@warID, @ownRank)) -- Get the next highest available opponent
		END
END_OF_PROCEDURE:
	-- Return the result of the function
RETURN @rankToAttack
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetClosestSecondAttackOpponent]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetClosestSecondAttackOpponent] 
(
	@warID int, 
	@ownRank int
)
RETURNS int
AS
BEGIN
	DECLARE @rankToAttack int
	DECLARE @alreadyAttacked bit = 0
		,@defeated bit = 0
		,@foundMatch bit = 0
		,@maxStars int = 0
		,@attacked BIT = 0
		,@result BIT = 0
		,@secondAttacksLeft int = 0
		,@numberOfParticipants INT = 0
		,@count INT = 0

		SET @attacked = (SELECT dbo.CompletedSecondAttack(@warID, @ownRank))
		IF (@attacked = 1) -- This player already did his second attack
		BEGIN
			SET @rankToAttack = 0
			GOTO END_OF_PROCEDURE		
		END

		-- We Favour a lower rank neighbour first
		-- See if there is any of this player's opponent's neighbours available for a first attack
		SET @rankToAttack =  (SELECT [dbo].GetAvailableNeighbour(@warID, @ownRank))
		IF (@rankToAttack > @ownRank) --Is the neighbour a lower rank
		BEGIN
			GOTO END_OF_PROCEDURE --We are done
		END

		-- Next we see if there is any lower ranks available
		SET @rankToAttack = (SELECT dbo.GetNextLowest(@warID, @ownRank)) -- Get the next lowest available opponent
		IF (@rankToAttack <> 0) -- There is a lower rank available to attack
		BEGIN
			GOTO END_OF_PROCEDURE --We are done
		END

		-- Next we see if he can attack his direct opponent
		SET @alreadyAttacked = (SELECT dbo.OwnDirectOpponentAttacked(@warID, @ownRank));
		IF (@alreadyAttacked = 0) --This player did not yet attack his direct opponent
		BEGIN
			SET @maxStars = (SELECT dbo.GetMaxStars(@warID, @ownRank)) --Check if anybody else already defeated his direct opponent
			IF (@maxStars >= 2)
				SET @defeated = 1;
		END

		-- Is his direct opponent available
		IF (@alreadyAttacked = 0 AND @defeated = 0) --This player's direct opponent is available for him to attack
		BEGIN
			SET @rankToAttack = @ownRank
			GOTO END_OF_PROCEDURE --We are done
		END

		-- No lower or equal rank available
		SET @rankToAttack = (SELECT dbo.GetNextHighest(@warID, @ownRank))  --So we go for the next highest opponent
END_OF_PROCEDURE:
RETURN @rankToAttack
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetEqualOrNextHighest]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetEqualOrNextHighest]
(
	@warID int,
	@opponent int
)
RETURNS int
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Rank int
	DECLARE @starsTaken int
	SET @starsTaken = (Select A.StarsTaken FROM (
		Select WarID, OurAttack, OurRank, MAX(ISNULL(StarsTaken,0)) AS StarsTaken 
		FROM View_StarsTaken GROUP BY OurAttack, WarID, OurRank) as A WHERE A.OurRank = @opponent AND A.WarID = @warID AND A.OurAttack = 1)
	IF (ISNULL(@starsTaken,0) <= 1)
	BEGIN
		SET @Rank = @opponent
	END
	ELSE
	BEGIN
		SET @Rank = (SELECT dbo.GetNextHighest(@warID, @opponent))
	END

	-- Return the result of the function
	RETURN @Rank

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetEqualOrNextLowest]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetEqualOrNextLowest]
(
	@warID int,
	@opponent int
)
RETURNS int
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Rank INT
	DECLARE @starsTaken int
	
	SET @starsTaken = (Select A.StarsTaken FROM (
		Select WarID, OurAttack, OurRank, MAX(StarsTaken) AS StarsTaken 
		FROM View_StarsTaken GROUP BY OurAttack, WarID, OurRank) as A 
		WHERE A.OurRank = @opponent AND A.WarID = @warID AND OurAttack = 1)
	IF (ISNULL(@starsTaken,0) <= 1)
		BEGIN
			SET @Rank = @opponent
		END
	ELSE
	BEGIN
		SET @Rank = (SELECT dbo.GetNextLowest(@warID, @opponent))
	END
	-- Return the result of the function
	RETURN @Rank

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetMaxStars]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetMaxStars] 
(
	@WarID int,
	@opponent int
)
RETURNS int
AS
BEGIN
	DECLARE @NumberOfStars INT
		SET @NumberOfStars = (
		Select A.StarsTaken FROM (
			Select WarID, OurAttack, TheirRank, MAX(StarsTaken) AS StarsTaken 
			FROM View_StarsTaken GROUP BY OurAttack, WarID, TheirRank) as A 
			WHERE A.WarID = @warID AND A.TheirRank = @opponent AND A.OurAttack = 1)
	IF (ISNULL(@NumberOfStars, -1) = -1)
	BEGIN
		SET @NumberOfStars = 0
	END

	RETURN @NumberOfStars

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetNextHighest]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetNextHighest] 
(
	@WarID int,
	@ourRank int
)
RETURNS int
AS
BEGIN
	DECLARE @counter INT = 0
		,@temp int = 0
		,@Rank int
		,@numberOfParticipants INT
		,@rankToCheck INT = 0
		,@starsToBeTaken INT = 0

	SET @numberOfParticipants = (SELECT [dbo].[GetNumberOfParticipants](@WarID))

	-- Check if there is any opponents left with 1 star to take
	IF @ourRank = 1
		RETURN 0
	SET @rankToCheck = @ourRank - 1
	WHILE @rankToCheck <= @numberOfParticipants
	BEGIN
		BEGIN
			SET @temp = (select StarsToBeWin from [View_StarsToBeWin] WHERE warid = @WarID and RANK = @rankToCheck)
			--SET @temp = (SELECT TOP(1) [Rank] FROM [COC].[dbo].[View_StarsToBeWin] WHERE WarID = @WarID AND Rank < @rankToCheck AND StarsToBeWin >= 1 ORDER BY [Rank] DESC)
			SET @starsToBeTaken = (SELECT ISNULL(@temp,0))
			IF @starsToBeTaken <> 0 -- There is 1 or more stars left from this opponent to take
			BEGIN
				-- check if this opponent has already been attacked by our rank
				IF (select [dbo].[HasTheirRankAlreadyBeenAttackedByOurRank](@WarID, @rankToCheck, @ourRank)) = 0
				BEGIN
					SET @Rank = @rankToCheck
					BREAK
				END
			END
		END
		SET @rankToCheck = @rankToCheck - 1
	END
	IF (@ourRank -1 > @Rank) -- The recommened target is more than 1 rank stronger than our opponent
	BEGIN
		SET @Rank = -1
	END
	RETURN @Rank
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetNextHighestOrEqual]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetNextHighestOrEqual] 
(
	@warID int,
	@opponent int
)
RETURNS int
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Rank INT
	DECLARE @starsTaken int

	SET @Rank = (SELECT dbo.GetNextHighest(@warID, @opponent))
	IF (@Rank = 0)
	BEGIN
		SET @starsTaken = (
			Select A.StarsTaken FROM (
				Select WarID, OurAttack, OurRank, MAX(StarsTaken) AS StarsTaken 
				FROM View_StarsTaken GROUP BY OurAttack, WarID, OurRank) as A 
				WHERE A.OurRank = @opponent AND A.WarID = @warID AND A.OurAttack = 1)
		IF (@starsTaken <= 1)
		BEGIN
			SET @Rank = @opponent
		END
	END
	IF (ISNULL(@Rank, -1) = -1)
	BEGIN
		SET @Rank = 0
	END

	-- Return the result of the function
	RETURN @Rank

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetNextLowest]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetNextLowest] 
(
	@WarID int,
	@ourRank int
)
RETURNS int
AS
BEGIN
	DECLARE @counter INT = 0
		,@temp int = 0
		,@Rank int
		,@numberOfParticipants INT
		,@rankToCheck INT = 0
		,@starsToBeTaken INT = 0

	SET @numberOfParticipants = (SELECT [dbo].[GetNumberOfParticipants](@WarID))

	SET @rankToCheck = @ourRank + 1
	WHILE @rankToCheck <= @numberOfParticipants
	BEGIN
		BEGIN
			SET @temp = (select StarsToBeWin from [View_StarsToBeWin] WHERE warid = @WarID and RANK = @rankToCheck)
			SET @starsToBeTaken = (SELECT ISNULL(@temp,0))
			IF @starsToBeTaken <> 0 -- There is 1 or more stars left from this opponent to take
			BEGIN
				IF (select [dbo].[HasTheirRankAlreadyBeenAttackedByOurRank](@WarID, @rankToCheck, @ourRank)) = 0
				BEGIN
					SET @Rank = @rankToCheck
					BREAK
				END
			END
		END
		SET @rankToCheck = @rankToCheck + 1
	END
	IF @rankToCheck > @numberOfParticipants
		SET @rankToCheck = 0
	RETURN @Rank
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetNextLowestOrEqual]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetNextLowestOrEqual]
(
	@warID int,
	@opponent int
)
RETURNS INT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Rank INT
	DECLARE @starsTaken int

	SET @Rank = (SELECT dbo.GetNextLowest(@warID, @opponent))
	IF (@Rank = 0)
	BEGIN
		SET @starsTaken = (
			Select A.StarsTaken FROM (
				Select WarID, OurAttack, OurRank, MAX(StarsTaken) AS StarsTaken 
				FROM View_StarsTaken GROUP BY OurAttack, WarID, OurRank) as A 
				WHERE A.OurRank = @opponent AND A.WarID = @warID AND OurAttack = 1)
		IF (@starsTaken <= 1)
		BEGIN
			SET @Rank = @opponent
		END
	END
	IF (ISNULL(@Rank, -1) = -1)
	BEGIN
		SET @Rank = 0
	END

	-- Return the result of the function
	RETURN @Rank

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetNumberOfAttacks]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetNumberOfAttacks]
(
	@warID INT
	,@ownRank INT
)
RETURNS INT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @numberOfAttacks INT
	SET @numberOfAttacks = (
		SELECT COUNT(dbo.OurParticipant.OurParticipantID) AS NumberOfAttacks
		FROM dbo.Attack INNER JOIN
			dbo.OurParticipant ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID
		WHERE (dbo.Attack.StarsTaken IS NOT NULL) AND (dbo.OurParticipant.WarID = @warID) AND (dbo.OurParticipant.Rank = @ownRank) AND dbo.Attack.OurAttack = 1)

	-- Return the result of the function
	RETURN @numberOfAttacks

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetNumberOfParticipants]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetNumberOfParticipants]
(
	@WarID INT
)
RETURNS INT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Result INT

	-- Add the T-SQL statements to compute the return value here
	SET @Result = (SELECT COUNT(*) FROM OurParticipant WHERE WarID = @WarID)

	-- Return the result of the function
	RETURN @Result

END

GO
/****** Object:  UserDefinedFunction [dbo].[GetSecondsAttacksLeftBelowRank]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetSecondsAttacksLeftBelowRank] 
(
	@warID int, 
	@ownRank int
)
RETURNS int
AS
BEGIN
	DECLARE @result INT = 0

	SET @result = (
		SELECT COUNT(*) AS Result
		FROM [COC].[dbo].[View_WarProgress]
		WHERE WarID = @warID AND FirstAttack = 0 AND OurRank > @ownRank AND StarsTaken IS NULL
	)
	-- Return the result of the function
	RETURN @result
END

GO
/****** Object:  UserDefinedFunction [dbo].[GetStarsTakenFromOpponent]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[GetStarsTakenFromOpponent] 
(
	@warID int, 
	@theirRank int
)
RETURNS int
AS
BEGIN
	DECLARE @result INT = 0

	SET @result = (
		SELECT ISNULL(MAX([StarsTaken]),0)
		FROM [COC].[dbo].[View_StarsTaken] WHERE WarID = @warID AND TheirRank = @theirRank
		GROUP BY WarID, TheirRank	)

	-- Return the result of the function
	RETURN @result
END

GO
/****** Object:  UserDefinedFunction [dbo].[HasTheirRankAlreadyBeenAttackedByOurRank]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[HasTheirRankAlreadyBeenAttackedByOurRank]
(
	@warID INT,
	@rankToCheck INT,
	@ourRank INT
)
RETURNS BIT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @result BIT = 0
		,@count INT = 0


	SET @count = (SELECT count(*) FROM [COC].[dbo].[View_StarsTaken] where ourrank = @ourRank and TheirRank = @rankToCheck and warid = @warID)
	IF @count > 0
		SET @result = 1		
	ELSE
		SET @result = 0
	RETURN @result
END

GO
/****** Object:  UserDefinedFunction [dbo].[IsNeighbourAvailable]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[IsNeighbourAvailable] 
(
	@WarID INT
	,@OwnRank INT
)
RETURNS INT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Result INT
	DECLARE @counter INT
		,@lowerNeighbour INT = 0
		,@higherNeighbour INT = 0
		,@numberOfStars INT

	-- Is the higher neighbour available?
	IF (@OwnRank > 1) -- This is the highest rank
	BEGIN
		SET @numberOfStars = (SELECT [dbo].[GetMaxStars](@WarID, @OwnRank - 1))
		IF (@numberOfStars < 2) --Is the next higher opponent available
			SET @higherNeighbour = @OwnRank - 1
	END

	-- Is the lower neighbour available?
	IF (@OwnRank < [dbo].[GetNumberOfParticipants](@WarID)) -- This is the lowest rank
	BEGIN
		SET @numberOfStars = (SELECT [dbo].[GetMaxStars](@WarID, @OwnRank + 1))
		IF (@numberOfStars < 2)
			SET @lowerNeighbour = @OwnRank + 1
	END

	-- Check the lower rank first
	IF (@lowerNeighbour > 0)
	BEGIN
		-- Check if this opponent has already been attacked by our rank
		SELECT @numberOfStars = (SELECT COUNT(*) FROM dbo.View_OurAttackedOpponents WHERE (WarID = @WarID) AND (OurRank = @OwnRank) AND (TheirRank = @lowerNeighbour))
		IF (@numberOfStars > 0)
			SET @Result = 0
		ELSE
			SET @Result = @lowerNeighbour
	END
	ELSE 
	BEGIN
		SELECT @numberOfStars = (SELECT COUNT(*) FROM dbo.View_OurAttackedOpponents WHERE (WarID = @WarID) AND (OurRank = @OwnRank) AND (TheirRank = @higherNeighbour))
		IF (@numberOfStars > 0)
			SET @Result = 0
		ELSE
			SET @Result = @higherNeighbour
	END
END_OF_FUNCTION:
	RETURN @Result
END

GO
/****** Object:  UserDefinedFunction [dbo].[IsNexLowerNeighbourAvailable]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[IsNexLowerNeighbourAvailable]
(
	@WarID INT
	,@OwnRank INT
)
RETURNS BIT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Result BIT
	DECLARE @counter INT
		,@numberOfParticipants INT = 0
		,@starsTaken INT = 0

	SET @numberOfParticipants = (SELECT [dbo].[GetNumberOfParticipants](@warID))
	IF (@OwnRank = @numberOfParticipants)
	BEGIN
		SET @Result = 0
		GOTO END_OF_PROCEDURE
	END

	SET @starsTaken = (
		SELECT MAX(StarsTaken) AS StarsTaken
		FROM dbo.View_StarsTaken
		WHERE (WarID = @warID)
		GROUP BY TheirRank
		HAVING	(TheirRank = @OwnRank + 1))
	IF (@starsTaken IS NULL OR @starsTaken < 2) --The next lower neighbour is available
	BEGIN
		SET @Result = 1
	END
	ELSE
	BEGIN
		SET @Result = 0
	END
END_OF_PROCEDURE:
RETURN @Result
END
GO
/****** Object:  UserDefinedFunction [dbo].[IsNextHigherNeighbourAvailable]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[IsNextHigherNeighbourAvailable]
(
	@WarID INT
	,@OwnRank INT
)
RETURNS BIT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Result BIT
	DECLARE @counter INT,
		@numberOfParticipants INT = 0,
		@starsTaken INT = 0

	IF (@OwnRank = 1)
	BEGIN
		SET @Result = 0
		GOTO END_OF_PROCEDURE
	END

	SET @starsTaken = (SELECT [dbo].[GetMaxStars](@WarID, @OwnRank - 1))	
	IF (@starsTaken < 2) --The next higher neighbour is available
	BEGIN
		SET @Result = 1
	END
	ELSE
	BEGIN
		SET @Result = 0
	END
END_OF_PROCEDURE:
	RETURN @Result
END

GO
/****** Object:  UserDefinedFunction [dbo].[OwnDirectOpponentAttacked]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE FUNCTION [dbo].[OwnDirectOpponentAttacked]
(
	@warID int,
	@ownRank int
)
RETURNS BIT
AS
BEGIN
	-- Declare the return variable here
	DECLARE @Result BIT
	DECLARE @counter int

	SET @counter = (
		--SELECT dbo.Attack.WarID, dbo.Attack.StarsTaken, dbo.OurParticipant.Rank AS OurRank, dbo.TheirParticipant.Rank AS TheirRank
		SELECT COUNT(*)
		FROM dbo.Attack INNER JOIN
			dbo.OurParticipant ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID INNER JOIN
			dbo.TheirParticipant ON dbo.Attack.TheirParticipantID = dbo.TheirParticipant.TheirParticipantID
		WHERE (dbo.Attack.OurAttack = 1) 
			AND (dbo.OurParticipant.Rank = @ownRank) 
			AND (dbo.Attack.WarID = @warID)  
			AND dbo.OurParticipant.Rank = dbo.TheirParticipant.Rank	
			AND (dbo.Attack.StarsTaken IS NOT NULL)
	)
	IF (@counter = 0)
	BEGIN
		SET @Result = 0
	END
	ELSE
	BEGIN
		SET @Result = 1
	END
	RETURN @Result
END

GO
/****** Object:  UserDefinedFunction [dbo].[PlayersNextBestAttack]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		<Author,,Name>
-- Create date: <Create Date, ,>
-- Description:	<Description, ,>
-- =============================================
CREATE FUNCTION [dbo].[PlayersNextBestAttack] 
(
	@warID INT
	,@ownRank INT
)
RETURNS int
AS
BEGIN
	DECLARE @attacksDone INT
		,@RankToAttack INT
		,@NeighbourattacksDone INT
		,@numberOfPlayers INT

	--Must player still do his first attack?

	SET @attacksDone = (SELECT [dbo].[GetNumberOfAttacks](@warID, @ownRank))

	IF (@attacksDone = 2) -- Check if both attacks were done
	BEGIN
		SET @RankToAttack = 0
		GOTO END_OF_PROCEDURE
	END
	IF (@attacksDone = 1) -- First attack done
	BEGIN
		GOTO SECOND_ATTACK
	END

	-- Check to see if this player got a lower neighbour available that was already attacked but less than 2 stars were taken
	--First lets see if this is the lowest rank player
	SET @numberOfPlayers = (SELECT [dbo].[GetNumberOfParticipants](@warID))
	IF (@ownRank <> @numberOfPlayers)
	BEGIN
		--Get the neighbours
		SET @RankToAttack = (SELECT [dbo].[GetAvailableNeighbour](@warID, @ownRank))
		IF (@ownRank < @RankToAttack) --Is this a lower rank opponent
			
		BEGIN
			-- This is a lower rank available neighbour that was already attacked but less than 2 stars were taken
			GOTO END_OF_PROCEDURE
		END
	END

	IF (@attacksDone = 0) -- Not done first attack
	BEGIN
		SET @RankToAttack = (SELECT dbo.GetClosestFirstAttackOpponent(@warID, @ownRank))
		GOTO END_OF_PROCEDURE
	END

SECOND_ATTACK:
	-- Must be second attack
	SET @RankToAttack = (SELECT dbo.GetClosestSecondAttackOpponent(@warID, @ownRank))
	IF (@RankToAttack = 0) --This player must still do one attack and 
		-- no one seems left.. Lets try and see if their is a higher neighbour available
	BEGIN
		SET @RankToAttack = (SELECT [dbo].[GetAvailableNeighbour](@warID, @ownRank))
	END

END_OF_PROCEDURE:
RETURN @RankToAttack
END
GO
/****** Object:  Table [dbo].[Attack]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Attack](
	[AttackID] [int] IDENTITY(1,1) NOT NULL,
	[WarID] [int] NOT NULL,
	[OurAttack] [bit] NOT NULL,
	[FirstAttack] [bit] NOT NULL,
	[OurParticipantID] [int] NOT NULL,
	[TheirParticipantID] [int] NULL,
	[StarsTaken] [int] NULL,
	[TimeOfAttack] [datetime] NULL,
	[NextRecommendedAttack] [int] NULL,
	[BusyAttackingRank] [int] NULL,
 CONSTRAINT [PK_Attack] PRIMARY KEY CLUSTERED 
(
	[AttackID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[gcm_users]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[gcm_users](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[gcm_regid] [text] NULL,
	[game_name] [varchar](50) NOT NULL,
	[email] [varchar](255) NULL,
	[created_at] [datetime] NOT NULL,
 CONSTRAINT [PK_gcm_users] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[OurParticipant]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[OurParticipant](
	[OurParticipantID] [int] IDENTITY(1,1) NOT NULL,
	[WarID] [int] NOT NULL,
	[PlayerID] [int] NOT NULL,
	[Experience] [int] NULL,
	[Rank] [int] NOT NULL,
	[TownHallLevel] [int] NOT NULL,
	[Active] [bit] NULL,
 CONSTRAINT [PK_OurParticipant] PRIMARY KEY CLUSTERED 
(
	[OurParticipantID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[Player]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[Player](
	[PlayerID] [int] IDENTITY(1,1) NOT NULL,
	[GameName] [varchar](50) NOT NULL,
	[RealName] [varchar](50) NOT NULL,
	[Active] [bit] NULL,
 CONSTRAINT [PK_Player] PRIMARY KEY CLUSTERED 
(
	[PlayerID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
SET ANSI_PADDING OFF
GO
/****** Object:  Table [dbo].[TheirParticipant]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TheirParticipant](
	[TheirParticipantID] [int] IDENTITY(1,1) NOT NULL,
	[WarID] [int] NOT NULL,
	[Experience] [int] NULL,
	[Rank] [int] NOT NULL,
	[TownHallLevel] [int] NOT NULL,
 CONSTRAINT [PK_TheirParticipant] PRIMARY KEY CLUSTERED 
(
	[TheirParticipantID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[Visits]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Visits](
	[id] [tinyint] NULL,
	[visits] [int] NOT NULL
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[War]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[War](
	[WarID] [int] IDENTITY(1,1) NOT NULL,
	[Date] [date] NOT NULL,
	[NumberOfParticipants] [int] NOT NULL,
	[WarsWeWon] [int] NULL,
	[WarsTheyWon] [int] NULL,
	[Active] [bit] NULL,
 CONSTRAINT [PK_War] PRIMARY KEY CLUSTERED 
(
	[WarID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  View [dbo].[View_WarProgress]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[View_WarProgress]
AS
SELECT     TOP (100) PERCENT dbo.Player.GameName, dbo.Attack.FirstAttack, dbo.Attack.StarsTaken, dbo.OurParticipant.Rank AS OurRank, 
                      dbo.TheirParticipant.Rank AS TheirRank, dbo.War.WarID, dbo.Attack.OurAttack, dbo.Attack.TimeOfAttack, dbo.OurParticipant.Active
FROM         dbo.Attack INNER JOIN
                      dbo.War INNER JOIN
                      dbo.OurParticipant ON dbo.War.WarID = dbo.OurParticipant.WarID INNER JOIN
                      dbo.Player ON dbo.OurParticipant.PlayerID = dbo.Player.PlayerID ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID LEFT OUTER JOIN
                      dbo.TheirParticipant ON dbo.Attack.TheirParticipantID = dbo.TheirParticipant.TheirParticipantID
WHERE     (dbo.Attack.OurAttack = 1)

GO
/****** Object:  View [dbo].[View_GetPlayersBelowWithTwoAttacksLeft]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[View_GetPlayersBelowWithTwoAttacksLeft]
AS
SELECT     Active, GameName, WarID, OurRank
FROM         (SELECT     GameName, Active, WarID, OurRank
                       FROM          dbo.View_WarProgress
                       WHERE      (OurAttack = 1) AND (StarsTaken IS NULL)) AS A
GROUP BY GameName, Active, WarID, OurRank
HAVING      (COUNT(*) = 2)


GO
/****** Object:  View [dbo].[View_OurAttackedOpponents]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[View_OurAttackedOpponents]
AS
SELECT     dbo.Attack.WarID, dbo.OurParticipant.Rank AS OurRank, dbo.TheirParticipant.Rank AS TheirRank, dbo.OurParticipant.OurParticipantID, dbo.Attack.StarsTaken
FROM         dbo.Attack LEFT OUTER JOIN
                      dbo.TheirParticipant ON dbo.Attack.TheirParticipantID = dbo.TheirParticipant.TheirParticipantID LEFT OUTER JOIN
                      dbo.OurParticipant ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID
WHERE     (dbo.Attack.OurAttack = 1)

GO
/****** Object:  View [dbo].[View_OurStatsVSTheirStats]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[View_OurStatsVSTheirStats]
AS
SELECT DISTINCT 
                      TOP (100) PERCENT dbo.TheirParticipant.WarID, dbo.Player.GameName, dbo.OurParticipant.Rank AS OurRank, dbo.OurParticipant.Experience AS OurExperience, 
                      dbo.OurParticipant.TownHallLevel AS OurTownhall, dbo.TheirParticipant.Rank AS TheirRank, dbo.TheirParticipant.Experience AS TheirExperience, 
                      dbo.TheirParticipant.TownHallLevel AS TheirTownhall
FROM         dbo.OurParticipant INNER JOIN
                      dbo.TheirParticipant ON dbo.OurParticipant.WarID = dbo.TheirParticipant.WarID AND dbo.OurParticipant.Rank = dbo.TheirParticipant.Rank INNER JOIN
                      dbo.Player ON dbo.OurParticipant.PlayerID = dbo.Player.PlayerID
ORDER BY dbo.TheirParticipant.WarID, OurRank

GO
/****** Object:  View [dbo].[View_StarsTaken]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[View_StarsTaken]
AS
SELECT     dbo.TheirParticipant.WarID, dbo.OurParticipant.Rank AS OurRank, dbo.TheirParticipant.Rank AS TheirRank, dbo.Attack.StarsTaken, dbo.Attack.OurAttack, 
                      dbo.Attack.FirstAttack
FROM         dbo.Attack INNER JOIN
                      dbo.TheirParticipant ON dbo.Attack.TheirParticipantID = dbo.TheirParticipant.TheirParticipantID INNER JOIN
                      dbo.OurParticipant ON dbo.Attack.OurParticipantID = dbo.OurParticipant.OurParticipantID

GO
/****** Object:  View [dbo].[View_StarsToBeWin]    Script Date: 2015/07/05 03:28:34 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[View_StarsToBeWin]
AS
SELECT     TOP (100) PERCENT dbo.Attack.WarID, 3 - MAX(ISNULL(dbo.Attack.StarsTaken, 0)) AS StarsToBeWin, dbo.TheirParticipant.Rank
FROM         dbo.TheirParticipant LEFT OUTER JOIN
                      dbo.Attack ON dbo.TheirParticipant.TheirParticipantID = dbo.Attack.TheirParticipantID
GROUP BY dbo.Attack.WarID, dbo.TheirParticipant.Rank

GO
ALTER TABLE [dbo].[Attack] ADD  CONSTRAINT [DF_Attack_BusyAttackingRank]  DEFAULT ((0)) FOR [BusyAttackingRank]
GO
ALTER TABLE [dbo].[gcm_users] ADD  CONSTRAINT [DF_gcm_users_created_at]  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[OurParticipant] ADD  CONSTRAINT [DF_OurParticipant_Active]  DEFAULT ((1)) FOR [Active]
GO
ALTER TABLE [dbo].[Attack]  WITH NOCHECK ADD  CONSTRAINT [FK_Attack_OurParticipant] FOREIGN KEY([OurParticipantID])
REFERENCES [dbo].[OurParticipant] ([OurParticipantID])
GO
ALTER TABLE [dbo].[Attack] NOCHECK CONSTRAINT [FK_Attack_OurParticipant]
GO
ALTER TABLE [dbo].[Attack]  WITH NOCHECK ADD  CONSTRAINT [FK_Attack_TheirParticipant] FOREIGN KEY([TheirParticipantID])
REFERENCES [dbo].[TheirParticipant] ([TheirParticipantID])
GO
ALTER TABLE [dbo].[Attack] NOCHECK CONSTRAINT [FK_Attack_TheirParticipant]
GO
ALTER TABLE [dbo].[OurParticipant]  WITH CHECK ADD  CONSTRAINT [FK_OurParticipant_Player] FOREIGN KEY([PlayerID])
REFERENCES [dbo].[Player] ([PlayerID])
GO
ALTER TABLE [dbo].[OurParticipant] CHECK CONSTRAINT [FK_OurParticipant_Player]
GO
ALTER TABLE [dbo].[OurParticipant]  WITH CHECK ADD  CONSTRAINT [FK_OurParticipant_War] FOREIGN KEY([WarID])
REFERENCES [dbo].[War] ([WarID])
GO
ALTER TABLE [dbo].[OurParticipant] CHECK CONSTRAINT [FK_OurParticipant_War]
GO
ALTER TABLE [dbo].[TheirParticipant]  WITH CHECK ADD  CONSTRAINT [FK_TheirParticipant_War] FOREIGN KEY([WarID])
REFERENCES [dbo].[War] ([WarID])
GO
ALTER TABLE [dbo].[TheirParticipant] CHECK CONSTRAINT [FK_TheirParticipant_War]
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane1', @value=N'[0E232FF0-B466-11cf-A24F-00AA00A3EFFF, 1.00]
Begin DesignProperties = 
   Begin PaneConfigurations = 
      Begin PaneConfiguration = 0
         NumPanes = 4
         Configuration = "(H (1[40] 4[20] 2[20] 3) )"
      End
      Begin PaneConfiguration = 1
         NumPanes = 3
         Configuration = "(H (1 [50] 4 [25] 3))"
      End
      Begin PaneConfiguration = 2
         NumPanes = 3
         Configuration = "(H (1 [50] 2 [25] 3))"
      End
      Begin PaneConfiguration = 3
         NumPanes = 3
         Configuration = "(H (4 [30] 2 [40] 3))"
      End
      Begin PaneConfiguration = 4
         NumPanes = 2
         Configuration = "(H (1 [56] 3))"
      End
      Begin PaneConfiguration = 5
         NumPanes = 2
         Configuration = "(H (2 [66] 3))"
      End
      Begin PaneConfiguration = 6
         NumPanes = 2
         Configuration = "(H (4 [50] 3))"
      End
      Begin PaneConfiguration = 7
         NumPanes = 1
         Configuration = "(V (3))"
      End
      Begin PaneConfiguration = 8
         NumPanes = 3
         Configuration = "(H (1[56] 4[18] 2) )"
      End
      Begin PaneConfiguration = 9
         NumPanes = 2
         Configuration = "(H (1 [75] 4))"
      End
      Begin PaneConfiguration = 10
         NumPanes = 2
         Configuration = "(H (1[66] 2) )"
      End
      Begin PaneConfiguration = 11
         NumPanes = 2
         Configuration = "(H (4 [60] 2))"
      End
      Begin PaneConfiguration = 12
         NumPanes = 1
         Configuration = "(H (1) )"
      End
      Begin PaneConfiguration = 13
         NumPanes = 1
         Configuration = "(V (4))"
      End
      Begin PaneConfiguration = 14
         NumPanes = 1
         Configuration = "(V (2))"
      End
      ActivePaneConfig = 0
   End
   Begin DiagramPane = 
      Begin Origin = 
         Top = 0
         Left = 0
      End
      Begin Tables = 
         Begin Table = "A"
            Begin Extent = 
               Top = 6
               Left = 38
               Bottom = 114
               Right = 205
            End
            DisplayFlags = 280
            TopColumn = 0
         End
      End
   End
   Begin SQLPane = 
   End
   Begin DataPane = 
      Begin ParameterDefaults = ""
      End
      Begin ColumnWidths = 9
         Width = 284
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
      End
   End
   Begin CriteriaPane = 
      Begin ColumnWidths = 12
         Column = 1440
         Alias = 900
         Table = 1170
         Output = 720
         Append = 1400
         NewValue = 1170
         SortType = 1350
         SortOrder = 1410
         GroupBy = 1350
         Filter = 1350
         Or = 1350
         Or = 1350
         Or = 1350
      End
   End
End
' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_GetPlayersBelowWithTwoAttacksLeft'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPaneCount', @value=1 , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_GetPlayersBelowWithTwoAttacksLeft'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane1', @value=N'[0E232FF0-B466-11cf-A24F-00AA00A3EFFF, 1.00]
Begin DesignProperties = 
   Begin PaneConfigurations = 
      Begin PaneConfiguration = 0
         NumPanes = 4
         Configuration = "(H (1[42] 4[21] 2[15] 3) )"
      End
      Begin PaneConfiguration = 1
         NumPanes = 3
         Configuration = "(H (1 [50] 4 [25] 3))"
      End
      Begin PaneConfiguration = 2
         NumPanes = 3
         Configuration = "(H (1 [50] 2 [25] 3))"
      End
      Begin PaneConfiguration = 3
         NumPanes = 3
         Configuration = "(H (4 [30] 2 [40] 3))"
      End
      Begin PaneConfiguration = 4
         NumPanes = 2
         Configuration = "(H (1 [56] 3))"
      End
      Begin PaneConfiguration = 5
         NumPanes = 2
         Configuration = "(H (2 [66] 3))"
      End
      Begin PaneConfiguration = 6
         NumPanes = 2
         Configuration = "(H (4 [50] 3))"
      End
      Begin PaneConfiguration = 7
         NumPanes = 1
         Configuration = "(V (3))"
      End
      Begin PaneConfiguration = 8
         NumPanes = 3
         Configuration = "(H (1[56] 4[18] 2) )"
      End
      Begin PaneConfiguration = 9
         NumPanes = 2
         Configuration = "(H (1 [75] 4))"
      End
      Begin PaneConfiguration = 10
         NumPanes = 2
         Configuration = "(H (1[66] 2) )"
      End
      Begin PaneConfiguration = 11
         NumPanes = 2
         Configuration = "(H (4 [60] 2))"
      End
      Begin PaneConfiguration = 12
         NumPanes = 1
         Configuration = "(H (1) )"
      End
      Begin PaneConfiguration = 13
         NumPanes = 1
         Configuration = "(V (4))"
      End
      Begin PaneConfiguration = 14
         NumPanes = 1
         Configuration = "(V (2))"
      End
      ActivePaneConfig = 0
   End
   Begin DiagramPane = 
      Begin Origin = 
         Top = 0
         Left = 0
      End
      Begin Tables = 
         Begin Table = "Attack"
            Begin Extent = 
               Top = 0
               Left = 35
               Bottom = 191
               Right = 201
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "TheirParticipant"
            Begin Extent = 
               Top = 110
               Left = 488
               Bottom = 218
               Right = 654
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "OurParticipant"
            Begin Extent = 
               Top = 6
               Left = 277
               Bottom = 114
               Right = 437
            End
            DisplayFlags = 280
            TopColumn = 0
         End
      End
   End
   Begin SQLPane = 
   End
   Begin DataPane = 
      Begin ParameterDefaults = ""
      End
      Begin ColumnWidths = 9
         Width = 284
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
      End
   End
   Begin CriteriaPane = 
      Begin ColumnWidths = 11
         Column = 1440
         Alias = 900
         Table = 1170
         Output = 720
         Append = 1400
         NewValue = 1170
         SortType = 1350
         SortOrder = 1410
         GroupBy = 1350
         Filter = 1350
         Or = 1350
         Or = 1350
         Or = 1350
      End
   End
End
' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_OurAttackedOpponents'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPaneCount', @value=1 , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_OurAttackedOpponents'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane1', @value=N'[0E232FF0-B466-11cf-A24F-00AA00A3EFFF, 1.00]
Begin DesignProperties = 
   Begin PaneConfigurations = 
      Begin PaneConfiguration = 0
         NumPanes = 4
         Configuration = "(H (1[40] 4[20] 2[20] 3) )"
      End
      Begin PaneConfiguration = 1
         NumPanes = 3
         Configuration = "(H (1[40] 4[35] 3) )"
      End
      Begin PaneConfiguration = 2
         NumPanes = 3
         Configuration = "(H (1[38] 2[21] 3) )"
      End
      Begin PaneConfiguration = 3
         NumPanes = 3
         Configuration = "(H (4 [30] 2 [40] 3))"
      End
      Begin PaneConfiguration = 4
         NumPanes = 2
         Configuration = "(H (1[56] 3) )"
      End
      Begin PaneConfiguration = 5
         NumPanes = 2
         Configuration = "(H (2 [66] 3))"
      End
      Begin PaneConfiguration = 6
         NumPanes = 2
         Configuration = "(H (4 [50] 3))"
      End
      Begin PaneConfiguration = 7
         NumPanes = 1
         Configuration = "(V (3))"
      End
      Begin PaneConfiguration = 8
         NumPanes = 3
         Configuration = "(H (1[56] 4[18] 2) )"
      End
      Begin PaneConfiguration = 9
         NumPanes = 2
         Configuration = "(H (1[47] 4) )"
      End
      Begin PaneConfiguration = 10
         NumPanes = 2
         Configuration = "(H (1[66] 2) )"
      End
      Begin PaneConfiguration = 11
         NumPanes = 2
         Configuration = "(H (4 [60] 2))"
      End
      Begin PaneConfiguration = 12
         NumPanes = 1
         Configuration = "(H (1) )"
      End
      Begin PaneConfiguration = 13
         NumPanes = 1
         Configuration = "(V (4))"
      End
      Begin PaneConfiguration = 14
         NumPanes = 1
         Configuration = "(V (2))"
      End
      ActivePaneConfig = 1
   End
   Begin DiagramPane = 
      Begin Origin = 
         Top = 0
         Left = 0
      End
      Begin Tables = 
         Begin Table = "OurParticipant"
            Begin Extent = 
               Top = 7
               Left = 391
               Bottom = 166
               Right = 551
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "TheirParticipant"
            Begin Extent = 
               Top = 11
               Left = 90
               Bottom = 169
               Right = 256
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "Player"
            Begin Extent = 
               Top = 0
               Left = 629
               Bottom = 108
               Right = 780
            End
            DisplayFlags = 280
            TopColumn = 0
         End
      End
   End
   Begin SQLPane = 
      PaneHidden = 
   End
   Begin DataPane = 
      Begin ParameterDefaults = ""
      End
      Begin ColumnWidths = 9
         Width = 284
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
      End
   End
   Begin CriteriaPane = 
      Begin ColumnWidths = 11
         Column = 1440
         Alias = 2190
         Table = 2700
         Output = 720
         Append = 1400
         NewValue = 1170
         SortType = 1350
         SortOrder = 1410
         GroupBy = 1350
         Filter = 1350
         Or = 1350
         Or = 1350
         Or = 1350
      End
   End
End
' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_OurStatsVSTheirStats'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPaneCount', @value=1 , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_OurStatsVSTheirStats'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane1', @value=N'[0E232FF0-B466-11cf-A24F-00AA00A3EFFF, 1.00]
Begin DesignProperties = 
   Begin PaneConfigurations = 
      Begin PaneConfiguration = 0
         NumPanes = 4
         Configuration = "(H (1[42] 4[21] 2[7] 3) )"
      End
      Begin PaneConfiguration = 1
         NumPanes = 3
         Configuration = "(H (1 [50] 4 [25] 3))"
      End
      Begin PaneConfiguration = 2
         NumPanes = 3
         Configuration = "(H (1 [50] 2 [25] 3))"
      End
      Begin PaneConfiguration = 3
         NumPanes = 3
         Configuration = "(H (4 [30] 2 [40] 3))"
      End
      Begin PaneConfiguration = 4
         NumPanes = 2
         Configuration = "(H (1 [56] 3))"
      End
      Begin PaneConfiguration = 5
         NumPanes = 2
         Configuration = "(H (2 [66] 3))"
      End
      Begin PaneConfiguration = 6
         NumPanes = 2
         Configuration = "(H (4 [50] 3))"
      End
      Begin PaneConfiguration = 7
         NumPanes = 1
         Configuration = "(V (3))"
      End
      Begin PaneConfiguration = 8
         NumPanes = 3
         Configuration = "(H (1[56] 4[18] 2) )"
      End
      Begin PaneConfiguration = 9
         NumPanes = 2
         Configuration = "(H (1 [75] 4))"
      End
      Begin PaneConfiguration = 10
         NumPanes = 2
         Configuration = "(H (1[66] 2) )"
      End
      Begin PaneConfiguration = 11
         NumPanes = 2
         Configuration = "(H (4 [60] 2))"
      End
      Begin PaneConfiguration = 12
         NumPanes = 1
         Configuration = "(H (1) )"
      End
      Begin PaneConfiguration = 13
         NumPanes = 1
         Configuration = "(V (4))"
      End
      Begin PaneConfiguration = 14
         NumPanes = 1
         Configuration = "(V (2))"
      End
      ActivePaneConfig = 0
   End
   Begin DiagramPane = 
      Begin Origin = 
         Top = 0
         Left = 0
      End
      Begin Tables = 
         Begin Table = "Attack"
            Begin Extent = 
               Top = 4
               Left = 321
               Bottom = 214
               Right = 487
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "TheirParticipant"
            Begin Extent = 
               Top = 0
               Left = 574
               Bottom = 156
               Right = 740
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "OurParticipant"
            Begin Extent = 
               Top = 6
               Left = 38
               Bottom = 206
               Right = 198
            End
            DisplayFlags = 280
            TopColumn = 0
         End
      End
   End
   Begin SQLPane = 
   End
   Begin DataPane = 
      Begin ParameterDefaults = ""
      End
      Begin ColumnWidths = 9
         Width = 284
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
      End
   End
   Begin CriteriaPane = 
      Begin ColumnWidths = 11
         Column = 2265
         Alias = 900
         Table = 1170
         Output = 720
         Append = 1400
         NewValue = 1170
         SortType = 1350
         SortOrder = 1410
         GroupBy = 1350
         Filter = 1350
         Or = 1350
         Or = 1350
         Or = 1350
      End
   End
End
' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_StarsTaken'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPaneCount', @value=1 , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_StarsTaken'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane1', @value=N'[0E232FF0-B466-11cf-A24F-00AA00A3EFFF, 1.00]
Begin DesignProperties = 
   Begin PaneConfigurations = 
      Begin PaneConfiguration = 0
         NumPanes = 4
         Configuration = "(H (1[40] 4[20] 2[20] 3) )"
      End
      Begin PaneConfiguration = 1
         NumPanes = 3
         Configuration = "(H (1 [50] 4 [25] 3))"
      End
      Begin PaneConfiguration = 2
         NumPanes = 3
         Configuration = "(H (1 [50] 2 [25] 3))"
      End
      Begin PaneConfiguration = 3
         NumPanes = 3
         Configuration = "(H (4[30] 2[40] 3) )"
      End
      Begin PaneConfiguration = 4
         NumPanes = 2
         Configuration = "(H (1 [56] 3))"
      End
      Begin PaneConfiguration = 5
         NumPanes = 2
         Configuration = "(H (2[57] 3) )"
      End
      Begin PaneConfiguration = 6
         NumPanes = 2
         Configuration = "(H (4 [50] 3))"
      End
      Begin PaneConfiguration = 7
         NumPanes = 1
         Configuration = "(V (3))"
      End
      Begin PaneConfiguration = 8
         NumPanes = 3
         Configuration = "(H (1[56] 4[18] 2) )"
      End
      Begin PaneConfiguration = 9
         NumPanes = 2
         Configuration = "(H (1 [75] 4))"
      End
      Begin PaneConfiguration = 10
         NumPanes = 2
         Configuration = "(H (1[66] 2) )"
      End
      Begin PaneConfiguration = 11
         NumPanes = 2
         Configuration = "(H (4 [60] 2))"
      End
      Begin PaneConfiguration = 12
         NumPanes = 1
         Configuration = "(H (1) )"
      End
      Begin PaneConfiguration = 13
         NumPanes = 1
         Configuration = "(V (4))"
      End
      Begin PaneConfiguration = 14
         NumPanes = 1
         Configuration = "(V (2))"
      End
      ActivePaneConfig = 0
   End
   Begin DiagramPane = 
      Begin Origin = 
         Top = 0
         Left = 0
      End
      Begin Tables = 
         Begin Table = "TheirParticipant"
            Begin Extent = 
               Top = 6
               Left = 38
               Bottom = 171
               Right = 220
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "Attack"
            Begin Extent = 
               Top = 6
               Left = 258
               Bottom = 175
               Right = 478
            End
            DisplayFlags = 280
            TopColumn = 0
         End
      End
   End
   Begin SQLPane = 
   End
   Begin DataPane = 
      Begin ParameterDefaults = ""
      End
      Begin ColumnWidths = 9
         Width = 284
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
      End
   End
   Begin CriteriaPane = 
      Begin ColumnWidths = 12
         Column = 1440
         Alias = 1860
         Table = 1170
         Output = 720
         Append = 1400
         NewValue = 1170
         SortType = 1350
         SortOrder = 1410
         GroupBy = 1350
         Filter = 1350
         Or = 1350
         Or = 1350
         Or = 1350
      End
   End
End
' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_StarsToBeWin'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPaneCount', @value=1 , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_StarsToBeWin'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane1', @value=N'[0E232FF0-B466-11cf-A24F-00AA00A3EFFF, 1.00]
Begin DesignProperties = 
   Begin PaneConfigurations = 
      Begin PaneConfiguration = 0
         NumPanes = 4
         Configuration = "(H (1[40] 4[20] 2[20] 3) )"
      End
      Begin PaneConfiguration = 1
         NumPanes = 3
         Configuration = "(H (1[50] 4[25] 3) )"
      End
      Begin PaneConfiguration = 2
         NumPanes = 3
         Configuration = "(H (1[50] 2[25] 3) )"
      End
      Begin PaneConfiguration = 3
         NumPanes = 3
         Configuration = "(H (4 [30] 2 [40] 3))"
      End
      Begin PaneConfiguration = 4
         NumPanes = 2
         Configuration = "(H (1[69] 3) )"
      End
      Begin PaneConfiguration = 5
         NumPanes = 2
         Configuration = "(H (2 [66] 3))"
      End
      Begin PaneConfiguration = 6
         NumPanes = 2
         Configuration = "(H (4 [50] 3))"
      End
      Begin PaneConfiguration = 7
         NumPanes = 1
         Configuration = "(V (3))"
      End
      Begin PaneConfiguration = 8
         NumPanes = 3
         Configuration = "(H (1[56] 4[18] 2) )"
      End
      Begin PaneConfiguration = 9
         NumPanes = 2
         Configuration = "(H (1[62] 4) )"
      End
      Begin PaneConfiguration = 10
         NumPanes = 2
         Configuration = "(H (1[66] 2) )"
      End
      Begin PaneConfiguration = 11
         NumPanes = 2
         Configuration = "(H (4 [60] 2))"
      End
      Begin PaneConfiguration = 12
         NumPanes = 1
         Configuration = "(H (1) )"
      End
      Begin PaneConfiguration = 13
         NumPanes = 1
         Configuration = "(V (4))"
      End
      Begin PaneConfiguration = 14
         NumPanes = 1
         Configuration = "(V (2))"
      End
      ActivePaneConfig = 0
   End
   Begin DiagramPane = 
      Begin Origin = 
         Top = -192
         Left = 0
      End
      Begin Tables = 
         Begin Table = "Attack"
            Begin Extent = 
               Top = 176
               Left = 632
               Bottom = 409
               Right = 798
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "War"
            Begin Extent = 
               Top = 6
               Left = 38
               Bottom = 163
               Right = 223
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "OurParticipant"
            Begin Extent = 
               Top = 211
               Left = 344
               Bottom = 396
               Right = 504
            End
            DisplayFlags = 280
            TopColumn = 1
         End
         Begin Table = "Player"
            Begin Extent = 
               Top = 291
               Left = 67
               Bottom = 413
               Right = 218
            End
            DisplayFlags = 280
            TopColumn = 0
         End
         Begin Table = "TheirParticipant"
            Begin Extent = 
               Top = 0
               Left = 971
               Bottom = 211
               Right = 1137
            End
            DisplayFlags = 280
            TopColumn = 1
         End
      End
   End
   Begin SQLPane = 
   End
   Begin DataPane = 
      Begin ParameterDefaults = ""
      End
      Begin ColumnWidths = 12
         Width = 284
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1500
         Width = 1980
         Width = 1500
         Width = 1500
         Width = 1500
      End
   End
   Begin CriteriaPane = 
   ' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_WarProgress'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPane2', @value=N'   Begin ColumnWidths = 11
         Column = 1440
         Alias = 900
         Table = 1170
         Output = 720
         Append = 1400
         NewValue = 1170
         SortType = 1350
         SortOrder = 1410
         GroupBy = 1350
         Filter = 1350
         Or = 1350
         Or = 1350
         Or = 1350
      End
   End
End
' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_WarProgress'
GO
EXEC sys.sp_addextendedproperty @name=N'MS_DiagramPaneCount', @value=2 , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'VIEW',@level1name=N'View_WarProgress'
GO
USE [master]
GO
ALTER DATABASE [COC] SET  READ_WRITE 
GO
