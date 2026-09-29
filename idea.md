# General:
- the app have name = 'Habit maker'
- the app has icon: 'https://icon-icons.com/icon/calendar/34472'
- this androind app target to a high performance application + the fastest app opening speed (kolin is recommended).

# IDEA:
- the app target to make daily task and return a reward if have done enough habit in a duration
- structure:
    - `reward`:
        - name
        - icon
        - note
    - `habit` (daily task):
        - name
        - icon (get from `https://phosphoricons.com/`)
        - color (brown from a fixed table of 15x5 table)
        - note
        - time:
            - startDate     (dd/mm/yyyy) (default = habit made date)
            - endDate       (dd/mm/yyyy) (optional)
        - reward:
            - weekly reward (optional, reward every week, optional):
                - reward id
                - Miss tolerance (e.g: if miss tolerance = 2 -> you can get weekly reward if done the habit 5 times)
            - monthly reward (optional, reward every month, optional):
                - reward id
                - Miss tolerance
            - final reward (reward after endDate, optional, only can be set if endDate is not none):
                - reward id 
                - Miss tolerance     
        - exception:
            - list day of week (e.g: not have to do this task at sunday of every week)
            - list day of month (e.g: not have to do this task at day 12 of every month)
            - list of fixed day (e.g: not have to do this task at 12/12 every year)

    - `habit record` (save for done/missed task or fixed note change):
        - ...
- idea:
    - reward is allowed to have duplicate name
    - each habit will be treat as daily action by default
    - time in habit allow a state: 'planned habit' (if current date < startDate)
    - exception of habit allow user to do not done the habit without increase `Miss tolerance`

# UI
include 5 screen with bottom navigation:
- home screen (show daily (past/current/future) habit (all of state: 'done' (in this day, not end of the whole habit),'in-process')), contain a navigation on the right and the content (habit row) on the left:
    - the left: contain 2 section 'In-process' and 'Done':
        - row layout:
            - the icon on the left of the row
            - the habit name on the right of the icon
            - the habit note on the bottom of the name (if is have note)
            - the `done`/`undone` button on the right of the row (`done` is a icon button with 'tick' icon, `undone` is the same but with 'x' icon) 
    - the right: include many part align in vertical:
        - the top: a donut chart for show <current daily done habits> / <current daily habits>
        - inside the donut chart: is 2 number align in vertical: <current daily done habits>, <current daily habits> seperate by a line
        - below the donut chart is <day-of-week> 
        - below the day-of-week is `<day-of-month>/month` (dd/mm), aligned center horizontal
        - below the date is the year align in the left
        - on the bottom of the home navigation is a `Now` button (for move to the current day)
    - change day:
        - by swipe the nabigation on the right of home screen, you can brown the past day/future day (the past day habit can be change (done/undone) but the future is not allowed to edit) 
- habit screen (show the list of all habit (contain 'done'/'planed'/'in-process') as each row stand for each habit):
    - the whole screen splited into 3 section (each section have a header at the top left of the section, each section will not have background or border but a gap between 2 section, the section still show when do not have habit, but it will show with the line `No habits remain`)
    - the section order is: `In-process`,`Planned`,`Done`
    - when click to the row, the app will just to other subscreen for edit habit
    - the row will have the same layout with row in home screen
    - the row is drag/drop able for swaping the habit order
- reward screen: each row stand for reward, click at one row will jump to edit popup (small popup jump from bottom (not center), and not subscreen)
- analysis screen (comming soon now)
- setting screen: for global config (languge, ...). *note: the app only have ONE THEME = light theme

# Other:
- you should reference the UI from this app `https://github.com/ngth1010v/Outgo`
- the screen can be switch by using bottom navigation or by swipe the page (the swipe logic can be get from the repo above)

# finally:
- if you meet:
    - the point you not understand in my ideas
    - the problem/conflict during building
    - any point that difference with my idea but better
-> ask me
- my pc already installed a android amulator, you should run it for test/debug when you done build an app
        

        