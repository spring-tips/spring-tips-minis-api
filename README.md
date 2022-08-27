#  Spring Tips: Twitter "Bites" Engine 

Scheduler and pipeline to produce and tweet tweet-sized Spring Tips 

## Design

## Happy Path 
This module is an HTTP application that responds to Github webhooks. When something changes in the Github repository, this application recieves a 
webhook. The webhook triggers a `git clone` and `git pull` of the git repository, which we then read, one Spring Tip XML file at a time, into a database. During this time, we could also perhaps proactively render the tweet and store the resulting bytes in the database, as well. Or, we could store the resulting bytes in the Git repository. Or S3. I just don't know, yet. S3 seems logical, but adds more moving parts. (Decisions, decisions...) Anyway, the result of this process should be to have all the tips in a database _and_ to update the schedule tips. 

A periodic `cron` or `ScheduledTask` will execute in the Spring application, periodically pulling down all the scheduled tips, turn them into tweets using the `twitter-gateway`, and then send them out if its the right time and they haven't yet been sent out.

### Preview Rendered Tips 
There has to be some way to preview what's going to be created. I propose building a simple, behind authentication, HTTP endpoint that takes a post (`/preview`?) and that returns the image fully-rendered. This endpoint should be HTTP REST-y. I should be able to then write a CLI, pass in a shared secret, and get the result I'm looking for to see how a given tip will look once tweeted. If it's a preview endpoint, maybe it could even validate whether the tweet text itself will fit and give back a zip file containing errors, the rendered image, and the generated SVG XML?


## Formatting 
This program uses the Spring Boot JavaFormat Maven plugin to ensure that the source code has the same formatting regardless of both how someone edits and in which IDE they edit it. It'll break the build if code is committed without first running the formatting plugin. Thus, before comitting every change, run: 

```shell 
mvn spring-javaformat:apply
```

## Motivation  

The goal of this project is to automate the generation of images to be included in periodic tweets promoting tips about Spring from the `@SpringTipsLive` (and eventually, from `@SpringCentral`) twitter handles. I'm working on figuring out how to programatically Tweet in another vein, so that's out of scope for this. Once I've figured that out, I'll integrate it with this. The scope of this program is simply to programatically take a database or Git repository of Tweets (images and code in folders, perhaps?) and then slide them into a design that supports templating. Maybe Scalable Vector Graphics (SVG, often encoded as `.svg`) could work?


## To Do 
- (x) figure out the right [ratios for images on twitter](https://influencermarketinghub.com/twitter-image-size/). It looks like 16:9 is the best, and that's great since that's what I'm doing. 
- since im doing images, what about instagram? and linkedin? i guess i could just manually schedule those or use an API with Hootsuite or something? is it worth trying out instagram for a little to see if this works with it? 
- (x) build the git integration so that the tips live in a git repository; build an endpoint to call from a github webhook whenever the git repository gets new content.  
  - setup a postgresql db for this thing. do i need to store the rendered tweets? It seems to me that whenever I submit a tip as a tweet, there won't be many images to handle at the same time, so I could just render the xml and the resulting `.jpg` on-demand, and avoid the cost of having to save the `.jpg` somewhere, like a SQL DB or a Git repository  (it'll eat into my quota, quickly, with each image taking half a megabyte!).
- how will the scheduler work? something that periodically scans the git repo and _upserts_ whatever directories are in the git repo to the DB? select max(date)::date + interval '7 days' or something like that. I'll need to do an "`upsert`" (`on conflict on constraint (blah) do update x = excluded.x...`).
- how will I build out the `/preview` endpoint? spring security, and a file upload that returns a `.zip` that i can use to diagnose everything?  
- how will I deploy this to kubernetes? Do I need another DNS name? A subdomain? Is this going to be the first service to live at `springtipslive.io`? 