program rogue;

{$APPTYPE CONSOLE}
uses Windows, SysUtils, Dialogs;

const wall   : char = chr(177);
      void   : char = ' '; // chr(219);
      pass   : char = chr(250);
      door   : char = chr(254);
      corpse : char = '%';
      chest  : char = chr(220);
      clip   : char = '=';
      flash  : char = chr(219);
      maxX            =  79; // map width
      maxY            =  21; // map height
      maxGuns         =   7; // amount of firearms
      maxTools        =   7; // amount of melee weapons
      maxMiscStuff    =   2; // misc items (dropped by monsters)
      maxMonsters     =  12; // amount of monsters
      maxDepth        =  30; // max level count
      fxDelay         =  25; // "animation" delay
      warpDelay       = 300; // warp timer (turns before recharge)
      medpackCapacity = 200; // medpack capacity
      medpackHealAmt  = 100; // medpack heal amount
      arMitigation    =  16; // armor mitigation value
      orbOfLifeValue  =  30; // orb of life value
      orbOfArmorValue =  12; // orb of armor value
      blindDuration   =   3; // flashbang duration
      LOGFILE = 'init.log';
      YELLOW  = FOREGROUND_RED or FOREGROUND_GREEN or FOREGROUND_INTENSITY;
      WHITE   = FOREGROUND_RED or FOREGROUND_GREEN or FOREGROUND_BLUE;

type tile = record
       ch       : char;
       revealed : boolean
     end;
     room = record
       x1       : byte; // left
       y1       : byte; // top
       x2       : byte; // right
       y2       : byte; // bottom
       revealed : boolean
     end;
     weapon = record
       name     : string;
       damage   : integer;
       modifier : integer
     end;
     ammu = record
       name     : string;
       weapon   : string;
       quantity : byte
     end;
     player = record
       x         : shortint;
       y         : shortint;
       face      : char;
       name      : string;
       life      : integer;
       armor     : integer;
       firearm   : weapon;
       tool      : weapon;
       ammo      : integer;
       pursuit   : boolean;
       blind_ctr : integer;
       state     : (dead, alive);
       prev_cell : char
     end;
     monster = record
       face      : char;
       name      : string;
       life      : integer;
       armor     : integer;
       weapon    : string;
       toughness : byte
     end;
     item = record
       x         : shortint;
       y         : shortint;
       name      : string;
       amount    : integer;
       tag       : char
     end;
     misc = record
       name      : string;
       value     : integer
     end;
     map         = array [0..maxX, 0..maxY] of tile;
     c_monsters  = array of player;
     c_rooms     = array of room;
     c_items     = array of item;
     c_queue     = array of array of integer;

var ConHandle    : THandle; // console window handle
    coord        : TCoord;  // cursor position
    CCI          : TConsoleCursorInfo;
    NOAW         : cardinal;
    msg_array    : array [0..2] of string;

const guns : array [0..maxGuns] of weapon = (
  (name     : 'empty';
   damage   : 0;
   modifier : 0),
  (name     : 'pistol';
   damage   : 10;
   modifier : 2),
  (name     : 'shotgun';
   damage   : 30;
   modifier : 7),
  (name     : 'rifle';
   damage   : 12;
   modifier : 3),
  (name     : 'SMG';
   damage   : 15;
   modifier : 4),
  (name     : 'assault rifle';
   damage   : 20;
   modifier : 5),
  (name     : 'minigun';
   damage   : 25;
   modifier : 3),
  (name     : 'hand cannon';
   damage   : 50;
   modifier : 0)
);

const ammo_types : array [0..maxGuns] of ammu = (
  (name     : 'empty';
   weapon   : 'empty';
   quantity : 0),
  (name     : 'pistol bullets';
   weapon   : 'pistol';
   quantity : 48),
  (name     : 'shotgun shells';
   weapon   : 'shotgun';
   quantity : 16),
  (name     : 'rifle rounds';
   weapon   : 'rifle';
   quantity : 48),
  (name     : 'SMG rounds';
   weapon   : 'SMG';
   quantity : 40),
  (name     : 'assault rifle rounds';
   weapon   : 'assault rifle';
   quantity : 32),
  (name     : 'minigun rounds';
   weapon   : 'minigun';
   quantity : 24),
  (name     : 'cannon projectiles';
   weapon   : 'hand cannon';
   quantity : 8)
);

const tools : array [0..maxTools] of weapon = (
  (name     : 'empty';
   damage   : 0;
   modifier : 0),
  (name     : 'fists';
   damage   : 5;
   modifier : 0),
  (name     : 'club';
   damage   : 7;
   modifier : 1),
  (name     : 'machete';
   damage   : 9;
   modifier : 2),
  (name     : 'shovel';
   damage   : 10;
   modifier : 2),
  (name     : 'hammer';
   damage   : 15;
   modifier : 2),
  (name     : 'maul';
   damage   : 20;
   modifier : 3),
  (name     : 'axe';
   damage   : 25;
   modifier : 5)
);

const misc_stuff : array [0..maxMiscStuff] of misc = (
  (name  : 'empty';
   value : 0),
  (name  : 'medkit';
   value : 10),
  (name  : 'armor shard';
   value : 1)  
);

const monster_types : array [1..maxMonsters] of monster = (
  (face      : 'g';
   name      : 'guard';
   life      : 5;
   armor     : 0;
   weapon    : 'club';
   toughness : 0),
  (face      : 'b';
   name      : 'security bot';
   life      : 7;
   armor     : 0;
   weapon    : 'SMG';
   toughness : 1),
  (face      : 'o';
   name      : 'officer';
   life      : 10;
   armor     : arMitigation;
   weapon    : 'machete';
   toughness : 1),
  (face      : 's';
   name      : 'sergeant';
   life      : 7;
   armor     : arMitigation;
   weapon    : 'pistol';
   toughness : 1),
  (face      : 'p';
   name      : 'peacekeeper';
   life      : 10;
   armor     : arMitigation;
   weapon    : 'SMG';
   toughness : 1),
  (face      : 'r';
   name      : 'rifleman';
   life      : 11;
   armor     : 0;
   weapon    : 'rifle';
   toughness : 1),
  (face      : 'B';
   name      : 'blademaster';
   life      : 30;
   armor     : arMitigation;
   weapon    : 'axe';
   toughness : 2),
  (face      : 'A';
   name      : 'assassin';
   life      : 30;
   armor     : arMitigation;
   weapon    : 'assault rifle';
   toughness : 2),
  (face      : 'W';
   name      : 'warleader';
   life      : 32;
   armor     : arMitigation;
   weapon    : 'shotgun';
   toughness : 2),
  (face      : 'M';
   name      : 'mercenary';
   life      : 35;
   armor     : arMitigation*2;
   weapon    : 'minigun';
   toughness : 3),
  (face      : 'C';
   name      : 'commander';
   life      : 40;
   armor     : arMitigation*2;
   weapon    : 'shotgun';
   toughness : 3),
  (face      : 'S';
   name      : 'Sentinel';
   life      : 256;
   armor     : arMitigation*10;
   weapon    : 'hand cannon';
   toughness : 255)
);

function GetConInputHandle: THandle;
begin
result:=GetStdHandle(STD_INPUT_HANDLE)
end; // handle for console input

function GetConOutputHandle: THandle;
begin
result:=GetStdHandle(STD_OUTPUT_HANDLE)
end; // handle for console output

procedure GotoXY(X, Y : word);
begin
coord.X:=X; coord.Y:=Y;
SetConsoleCursorPosition(ConHandle, coord)
end; // position cursor at x, y

procedure cls;
begin
coord.X:=0; coord.Y:=0;
FillConsoleOutputCharacter(ConHandle, ' ', 80*25, coord, NOAW);
GotoXY(0, 0)
end; // clear screen

procedure ShowCursor(show : bool);
begin
CCI.bVisible:=show;
SetConsoleCursorInfo(ConHandle, CCI)
end; // show/hide cursor

function ConProc(CtrlType : DWord) : bool; stdcall; far;
var s : string;
begin
case CtrlType of
     CTRL_C_EVENT        : s:='CTRL_C_EVENT';
     CTRL_BREAK_EVENT    : s:='CTRL_BREAK_EVENT';
     CTRL_CLOSE_EVENT    : s:='CTRL_CLOSE_EVENT';
     CTRL_LOGOFF_EVENT   : s:='CTRL_LOGOFF_EVENT';
     CTRL_SHUTDOWN_EVENT : s:='CTRL_SHUTDOWN_EVENT';
     else
                           s:='UNKNOWN_EVENT'
end;
MessageBox(0, pchar(s+' detected'), 'win32 console', MB_OK);
result:=true
end; // console event handler

function readkey : word;
var IBuff        : TInputRecord;
    IEvent       : DWord;
begin
repeat
  ReadConsoleInput(GetConInputHandle, IBuff, 1, IEvent)
until IBuff.Event.KeyEvent.bKeyDown;
result:=IBuff.Event.KeyEvent.wVirtualKeyCode
end; // wait until key is pressed, return key code

function capitalize(st : string) : string;
var i : integer;
begin
result:=upcase(st[1]);
if length(st)>1 then
  for i:=2 to length(st) do result:=result+st[i]
end; // capitalize first word

procedure process_messages;
var mess : msg;
begin
while PeekMessage(mess, 0, 0, 0, PM_REMOVE) do begin
  TranslateMessage(mess);
  DispatchMessage(mess)
end
end; // process messages for smoother performance

procedure void_map(var level : map);
var i, j : byte;
begin
for i:=0 to maxY do
  for j:=0 to maxX do begin
    level[j, i].ch:=void;
    level[j, i].revealed:=false
  end
end; // fill map with void

procedure add_room(x1, y1, x2, y2 : byte; var level : map);
var i, j : byte;
begin
for i:=x1 to x2 do
  for j:=y1 to y2 do
    if ((i=x1) or (i=x2)) or ((j=y1) or (j=y2)) then level[i, j].ch:=wall
                                                else level[i, j].ch:=pass
end; // put room into map

procedure reveal_room(x1, y1, x2, y2 : byte; var level : map);
var i, j : byte;
begin
for i:=x1 to x2 do
  for j:=y1 to y2 do level[i, j].revealed:=true
end; // reveal a room

procedure draw_map(var level : map; var rooms : c_rooms; hero : player);
var charPos      : TCoord;
    charBufSize  : TCoord;
    conWriteArea : TSmallRect;
    conBuffer    : array [0..maxY, 0..maxX] of TCharInfo;
    x, y, i      : byte;
begin
charPos.X:=0; charPos.Y:=0;
charBufSize.X:=maxX+1; charBufSize.Y:=maxY+1;
conWriteArea.Left:=0; conWriteArea.Top:=0;
conWriteArea.Right:=maxX; conWriteArea.Bottom:=maxY;
for x:=hero.x-1 to hero.x+1 do
  for y:=hero.y-1 to hero.y+1 do
    level[x, y].revealed:=true;
for x:=0 to maxX do
  for y:=0 to maxY do begin
    conBuffer[y, x].AsciiChar:=level[x, y].ch;
    for i:=0 to length(rooms)-1 do begin
      if ((x in [rooms[i].x1..rooms[i].x2]) and
          (y in [rooms[i].y1..rooms[i].y2]) and
          (hero.x in [rooms[i].x1+1..rooms[i].x2-1]) and
          (hero.y in [rooms[i].y1+1..rooms[i].y2-1])) then begin
        conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_GREEN or
                                    FOREGROUND_BLUE or FOREGROUND_INTENSITY;
        if level[x, y].ch=monster_types[maxMonsters].face then
          conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_BLUE or
                                      FOREGROUND_INTENSITY;
        if level[x, y].ch=corpse then
          conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_INTENSITY;
        if level[x, y].ch=chest then
          conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_GREEN or
                                      FOREGROUND_INTENSITY;;
        if level[x, y].ch=clip then
          conBuffer[y, x].Attributes:=FOREGROUND_BLUE or FOREGROUND_GREEN or
                                      FOREGROUND_INTENSITY;;
        reveal_room(rooms[i].x1, rooms[i].y1, rooms[i].x2, rooms[i].y2, level);
        rooms[i].revealed:=true; break
      end;
      if ((x in [rooms[i].x1..rooms[i].x2]) and
          (y in [rooms[i].y1..rooms[i].y2]) and
          (rooms[i].revealed)) then begin
        conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_GREEN or
                                    FOREGROUND_BLUE;
        if level[x, y].ch=monster_types[maxMonsters].face then
          conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_BLUE;
        if level[x, y].ch=corpse then
          conBuffer[y, x].Attributes:=FOREGROUND_RED;
        if level[x, y].ch=chest then
          conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_GREEN;
        if level[x, y].ch=clip then
          conBuffer[y, x].Attributes:=FOREGROUND_BLUE or FOREGROUND_GREEN;
        break
      end;
      if level[x, y].revealed then begin
        conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_GREEN or
                                    FOREGROUND_BLUE;
        if level[x, y].ch=monster_types[maxMonsters].face then
          conBuffer[y, x].Attributes:=FOREGROUND_RED or FOREGROUND_BLUE;
        if level[x, y].ch=corpse then
          conBuffer[y, x].Attributes:=FOREGROUND_RED;
        if level[x, y].ch=clip then
          conBuffer[y, x].Attributes:=FOREGROUND_BLUE or FOREGROUND_GREEN
      end else
        conBuffer[y, x].Attributes:=0
    end // rooms loop
  end; // (x, y)
WriteConsoleOutputA(ConHandle, @conBuffer, charBufSize, charPos, conWriteArea)
end; // draw map

procedure log_map(level : map);
var i, j : byte;
    log  : textfile;
begin
assignfile(log, LOGFILE);
if not FileExists(LOGFILE) then rewrite(log)
                           else append(log);
for j:=0 to maxY do begin
  for i:=0 to maxX do
    write(log, level[i, j].ch);
  writeln(log)
end;
writeln(log); flush(log); closefile(log)
end; // write map into log file

procedure log_creatures(hero : player; monsters : c_monsters);
var i   : integer;
    log : textfile;
begin
assignfile(log, LOGFILE);
if not FileExists(LOGFILE) then rewrite(log)
                           else append(log);
writeln(log, ' (', hero.x, ', ', hero.y, ') ', hero.face, ' ', hero.name, ' ',
             hero.prev_cell);
writeln(log, ' Life: ', hero.life);
writeln(log, ' Armor: ', hero.armor);
writeln(log, ' Firearm: ', hero.firearm.name);
writeln(log, ' Melee weapon: ', hero.tool.name);
writeln(log, ' Ammo: ', hero.ammo);
writeln(log);
for i:=0 to length(monsters)-1 do begin
  write(log, i+1, ': (', monsters[i].x, ', ', monsters[i].y, ') ');
  write(log, monsters[i].face, ' ', monsters[i].name, ' ', monsters[i].prev_cell, '; ');
  write(log, ' Life: ', monsters[i].life, '; ');
  write(log, ' Armor: ', monsters[i].armor, '; ');
  write(log, ' Firearm: ', monsters[i].firearm.name, '; ');
  writeln(log, ' Melee weapon: ', monsters[i].tool.name);
end;
writeln(log); flush(log); closefile(log)
end; // write creatures data into log file

procedure log_rooms(rooms : c_rooms; depth : byte);
var i   : byte;
    log : textfile;
begin
assignfile(log, LOGFILE);
if not FileExists(LOGFILE) then rewrite(log)
                           else append(log);
writeln(log, 'Depth: ', depth);
for i:=0 to length(rooms)-1 do begin
  writeln(log, i+1, ': (', rooms[i].x1, ':', rooms[i].y1, ') (',
                           rooms[i].x2, ':', rooms[i].y2, ') ')
end;
writeln(log); flush(log); closefile(log)
end; // log rooms coords

function generate_map(var level : map; var rooms : c_rooms;
                      var room_count : byte) : byte;
var column_count : byte;
    rx, ry       : byte;
    tempX        : byte;
begin
rx:=0; ry:=0; room_count:=0; tempX:=0; column_count:=0;
while true do begin
  setlength(rooms, room_count+1);
  with rooms[room_count] do begin
    revealed:=false;
    x1:=rx;
    y1:=ry;
    x2:=rx+random(35)+4;
    y2:=ry+random(17)+4;
    if x2>maxX then x2:=maxX;
    if y2>maxY then y2:=maxY;
    if x2>tempX then tempX:=x2;
    if y2<=(maxY-6) then ry:=y2+2
                    else begin ry:=0; inc(column_count) end;
    if (x2<=(maxX-6)) and (ry=0) then rx:=tempX+2;
    add_room(x1, y1, x2, y2, level);
    inc(room_count);
    if (tempX>(maxX-6)) and (y2>(maxY-6)) then break
  end;
end; // fill map with rooms
result:=column_count
end; // generate map layout

function build_map(var level : map; filename : string) : byte;
var fname          : textfile;
    st             : string;
    x1, y1, x2, y2 : byte;
    room_count     : byte;
begin
result:=0; room_count:=0;
if not FileExists(filename) then exit;
assignfile(fname, filename); reset(fname);
while not eof(fname) do begin
  readln(fname, st);
  if st='' then continue;
  room_count:=strtoint(copy(st, 1, pos(':', st)-1));
  delete(st, 1, pos('(', st));
  x1:=strtoint(copy(st, 1, pos(':', st)-1));
  delete(st, 1, pos(':', st));
  y1:=strtoint(copy(st, 1, pos(')', st)-1));
  delete(st, 1, pos('(', st));
  x2:=strtoint(copy(st, 1, pos(':', st)-1));
  delete(st, 1, pos(':', st));
  y2:=strtoint(copy(st, 1, pos(')', st)-1));
  add_room(x1, y1, x2, y2, level)
end;
closefile(fname);
result:=room_count
end; // build map with coords from file

function read_map(var level : map; filename : string) : byte;
var st   : string;
    f    : textfile;
    x, y : byte;
begin
result:=0;
if not FileExists(filename) then exit;
assignfile(f, filename); reset(f);
y:=0;
while not eof(f) do begin
  readln(f, st);
  if st='' then continue;
  for x:=1 to length(st) do level[x-1, y].ch:=st[x];
  y:=y+1
end;
end; // read map from file

procedure connect_rooms(var level : map);
var px, py, cx, tx, i : byte;
begin
px:=2; py:=maxY;
while true do begin
  while py>5 do begin // bottom -> top sequence
    while level[px, py].ch<>wall do py:=py-1;
    while (level[px, py-1].ch=pass) and (py>0) do py:=py-1;
    if level[px, py].ch=pass then py:=py-1; // (px-2, py) = top left corner
    if py<6 then break;
    cx:=px;
    while (level[cx, py].ch=wall) and (cx<maxX) do cx:=cx+1;
    if level[cx, py].ch<>wall then cx:=cx-3
                              else cx:=cx-2; // (cx, py) = top right passable
    while not ((level[cx+1, py+1].ch=pass) and
               (level[cx, py+1].ch=pass) and
               (level[cx-1, py+1].ch=pass) and
               (level[cx+1, py-3].ch=pass) and
               (level[cx, py-3].ch=pass) and
               (level[cx-1, py-3].ch=pass)) do cx:=cx-1; // looking for connectable spot
    level[cx, py].ch:=pass;
    level[cx, py-1].ch:=pass;
    level[cx, py-2].ch:=pass;
    level[cx-1, py-1].ch:=wall;
    level[cx+1, py-1].ch:=wall;
    py:=py-2;
    while (level[px, py-1].ch=pass) and (py>0) do py:=py-1;
    if level[px, py].ch=pass then py:=py-1; // (px, py) = top left passable
    if py<6 then break;
    level[px, py].ch:=pass;
    level[px, py-1].ch:=pass;
    level[px, py-2].ch:=pass;
    level[px-1, py-1].ch:=wall;
    level[px+1, py-1].ch:=wall;
    py:=py-2
  end;
  py:=0; // left -> right sequence
  while (level[px, py].ch=wall) and (px<maxX) do px:=px+1;
  if level[px, py].ch<>wall then px:=px-1; // rightmost wall
  if px>(maxX-6) then break;
  cx:=px+1;
  while (level[cx, py].ch<>wall) and (cx<maxX) do cx:=cx+1; // (cx, py) = next set of rooms' top left
  if level[cx, py].ch<>wall then break;
  level[px, py+2].ch:=pass;
  for i:=px+1 to cx-1 do begin
    level[i, py+1].ch:=wall;
    level[i, py+2].ch:=pass;
    level[i, py+3].ch:=wall
  end;
  level[cx, py+2].ch:=pass;
  px:=cx+2; py:=0; cx:=px; // top -> bottom sequence
  while py<(maxY-5) do begin
    while (level[cx, py].ch=wall) and (cx<maxX) do cx:=cx+1;
    if level[cx, py].ch<>wall then cx:=cx-1; // right wall
    while (level[cx, py].ch=wall) and (py<maxY) do py:=py+1;
    if level[cx, py].ch<>wall then py:=py-1; // (cx, py) = bottom right corner
    if py>(maxY-6) then break;
    cx:=cx-2; // possible passable (rightmost)
    while not ((level[cx+1, py-1].ch=pass) and
               (level[cx, py-1].ch=pass) and
               (level[cx-1, py-1].ch=pass) and
               (level[cx+1, py+3].ch=pass) and
               (level[cx, py+3].ch=pass) and
               (level[cx-1, py+3].ch=pass)) do cx:=cx-1; // (cx, py+1) = connectable spot
    level[cx, py].ch:=pass;
    level[cx, py+1].ch:=pass;
    level[cx, py+2].ch:=pass;
    level[cx-1, py+1].ch:=wall;
    level[cx+1, py+1].ch:=wall;
    py:=py+2;
    while (level[px-2, py].ch=wall) and (py<maxY) do py:=py+1;
    if level[px-2, py].ch<>wall then py:=py-1; // bottom wall
    if py>(maxY-6) then break;
    level[px, py].ch:=pass;
    level[px, py+1].ch:=pass;
    level[px, py+2].ch:=pass;
    level[px-1, py+1].ch:=wall;
    level[px+1, py+1].ch:=wall;
    py:=py+2; cx:=px+1
  end;
  py:=0; tx:=px-2; // right -> left sequence
  while (level[px, py].ch=wall) and (px<maxX) do px:=px+1;
  if px>(maxX-5) then break;
  while (level[px, py].ch<>wall) and (px<maxX) do px:=px+1;
  if px>(maxX-4) then break;
  if level[px, py].ch<>wall then px:=px-1; py:=maxY; // next set
  while level[px, py].ch<>wall do py:=py-1; // (px, py) = bottom left corner
  py:=py-2; // (px, py) = passable spot
  cx:=px-2; // (cx, py) = ~possible~ previous set's passable spot
  while true do begin
    while (level[cx, py].ch<>wall) and (cx>tx) do cx:=cx-1; // previous set's right wall
    if cx=tx then begin
      py:=py-1;
      cx:=px-2;
      continue
    end;
    if ((level[px+1, py-1].ch=pass) and
        (level[px+1, py].ch=pass) and
        (level[px+1, py+1].ch=pass) and
        (level[cx-1, py-1].ch=pass) and
        (level[cx-1, py].ch=pass) and
        (level[cx-1, py+1].ch=pass) and
        (level[cx+1, py-1].ch=void) and
        (level[cx+1, py].ch=void) and
        (level[cx+1, py+1].ch=void)) then begin
      level[cx, py].ch:=pass;
      for i:=cx+1 to px-1 do begin
        level[i, py-1].ch:=wall;
        level[i, py].ch:=pass;
        level[i, py+1].ch:=wall
      end;
      level[px, py].ch:=pass;
      break
    end;
    py:=py-1; cx:=px-2
  end;
  px:=px+2; py:=maxY
end // main loop
end; // connect rooms in map

procedure position_player(var level : map; var hero : player);
var px, py : byte;
begin
px:=1; py:=maxY;
while level[px, py].ch<>pass do py:=py-1;
hero.x:=px;
hero.y:=py;
level[px, py].ch:=hero.face
end; // player starting position

function position_exit(var level : map; column_count : byte) : TCoord;
var px, py : byte;
begin
px:=maxX; py:=0;
if odd(column_count) then begin
  while level[px, py].ch<>wall do px:=px-1;
  px:=px-2
end else begin
  while level[px, py].ch<>wall do px:=px-1;
  while level[px, py].ch=wall do px:=px-1;
  px:=px+1; // (px, py) = last set of rooms top left
  py:=maxY;
  while level[px, py].ch<>wall do py:=py-1; // bottom left
  while (level[px, py].ch=wall) and (px<maxX) do px:=px+1;
  if level[px, py].ch<>wall then px:=px-3
                         else px:=px-2
end;
level[px, py].ch:=door;
result.X:=px; result.Y:=py
end; // level exit

procedure position_monsters(var level : map; rooms : c_rooms;
                            var monsters : c_monsters; hero : player;
                            depth : byte; room_count : byte);
var i, j, k       : integer;
    x, y          : byte;
    the_room      : array [1..2] of TCoord;
    max_toughness : byte;
begin
x:=0; y:=0;
for i:=0 to length(rooms)-1 do
  if (hero.x in [rooms[i].x1+1..rooms[i].x2-1]) and
     (hero.y in [rooms[i].y1+1..rooms[i].y2-1]) then begin
    the_room[1].X:=rooms[i].x1+1; the_room[1].Y:=rooms[i].y1+1;
    the_room[2].X:=rooms[i].x2-1; the_room[2].Y:=rooms[i].y2-1;
    break
  end;
if depth=1 then begin // first level
  setlength(monsters, 10);
  for i:=0 to 9 do begin
    while level[x, y].ch<>pass do begin
      x:=random(maxX-1)+1;
      y:=random(maxY-1)+1
    end;
    monsters[i].x:=x;
    monsters[i].y:=y;
    monsters[i].face:=monster_types[1].face;
    monsters[i].name:=monster_types[1].name;
    monsters[i].life:=monster_types[1].life;
    monsters[i].armor:=monster_types[1].armor;
    monsters[i].firearm:=guns[0];
    for j:=1 to maxGuns do
      if guns[j].name=monster_types[1].weapon then monsters[i].firearm:=guns[j];
    monsters[i].tool:=tools[0];
    for j:=1 to maxTools do
      if tools[j].name=monster_types[1].weapon then monsters[i].tool:=tools[j];
    if (monsters[i].x in [the_room[1].X..the_room[2].X]) and
       (monsters[i].y in [the_room[1].Y..the_room[2].Y])
    then monsters[i].pursuit:=true
    else monsters[i].pursuit:=false;
    monsters[i].blind_ctr:=blindDuration;
    monsters[i].state:=alive;
    monsters[i].prev_cell:=pass;
    level[x, y].ch:=monsters[i].face
  end;
exit
end;
setlength(monsters, room_count+depth); // other levels
for i:=0 to room_count+depth-1 do begin
  while level[x, y].ch<>pass do begin
    x:=random(maxX-1)+1;
    y:=random(maxY-1)+1
  end;
  case depth of
        1..10 : max_toughness:=1;
       11..20 : max_toughness:=2
           else max_toughness:=3
  end;
  repeat
    k:=random(maxMonsters-1)+1
  until monster_types[k].toughness<=max_toughness;
  monsters[i].x:=x;
  monsters[i].y:=y;
  monsters[i].face:=monster_types[k].face;
  monsters[i].name:=monster_types[k].name;
  monsters[i].life:=monster_types[k].life;
  monsters[i].armor:=monster_types[k].armor;
  monsters[i].firearm:=guns[0];
  for j:=1 to maxGuns do
    if guns[j].name=monster_types[k].weapon then monsters[i].firearm:=guns[j];
  monsters[i].tool:=tools[0];
  for j:=1 to maxTools do
    if tools[j].name=monster_types[k].weapon then monsters[i].tool:=tools[j];
  if (monsters[i].x in [the_room[1].X..the_room[2].X]) and
     (monsters[i].y in [the_room[1].Y..the_room[2].Y])
  then monsters[i].pursuit:=true
  else monsters[i].pursuit:=false;
  monsters[i].blind_ctr:=blindDuration;
  monsters[i].state:=alive;
  monsters[i].prev_cell:=pass;
  level[x, y].ch:=monsters[i].face
end;
if depth=maxDepth then begin // final boss
  setlength(monsters, length(monsters)+1);
  i:=length(monsters)-1;
  while level[x, y].ch<>pass do begin
    x:=random(maxX-1)+1;
    y:=random(maxY-1)+1
  end;
  monsters[i].x:=x;
  monsters[i].y:=y;
  monsters[i].face:=monster_types[maxMonsters].face;
  monsters[i].name:=monster_types[maxMonsters].name;
  monsters[i].life:=monster_types[maxMonsters].life;
  monsters[i].armor:=monster_types[maxMonsters].armor;
  monsters[i].firearm:=guns[0];
  for j:=1 to maxGuns do
    if guns[j].name=monster_types[maxMonsters].weapon then monsters[i].firearm:=guns[j];
  monsters[i].tool:=tools[0];
  for j:=1 to maxTools do
    if tools[j].name=monster_types[maxMonsters].weapon then monsters[i].tool:=tools[j];
  monsters[i].pursuit:=false;
  monsters[i].blind_ctr:=blindDuration;
  monsters[i].state:=alive;
  monsters[i].prev_cell:=pass;
  level[x, y].ch:=monsters[i].face
end
end; // generate and put monsters into map

procedure position_chests(var level : map; rooms : c_rooms; var items : c_items);
var x, y   : shortint;
    i, j   : byte;
    rng, c : byte;
begin
i:=0; rng:=random(100);
if rng<10 then c:=3
          else c:=2;
setlength(items, length(items)+c);
while i<c do begin
  x:=random(maxX-2)+1;
  y:=random(maxY-2)+1;
  for j:=0 to length(rooms)-1 do
    if (x in [rooms[j].x1+1..rooms[j].x2-1]) and
       (y in [rooms[j].y1+1..rooms[j].y2-1]) and
       (level[x, y].ch=pass) then begin
      level[x, y].ch:=chest;
      items[length(items)-c+i].x:=x;
      items[length(items)-c+i].y:=y;
      items[length(items)-c+i].name:='unknown';
      break
    end;
  if level[x, y].ch=chest then i:=i+1
end
end; // generate some treasure chests

procedure clear_items(var items : c_items);
var i    : integer;
    mine : c_items;
    amnt : integer;
begin
if length(items)<1 then exit;
amnt:=0;
for i:=0 to length(items)-1 do
  if items[i].x=0 then begin
    inc(amnt);
    setlength(mine, amnt);
    mine[amnt-1].x:=0;
    mine[amnt-1].y:=0;
    mine[amnt-1].name:=items[i].name;
    mine[amnt-1].amount:=items[i].amount;
    mine[amnt-1].tag:=items[i].tag
  end;
setlength(items, length(mine));
if length(mine)<1 then exit;
for i:=0 to length(mine)-1 do begin
  items[i].x:=mine[i].x;
  items[i].y:=mine[i].y;
  items[i].name:=mine[i].name;
  items[i].amount:=mine[i].amount;
  items[i].tag:=mine[i].tag
end
end; // delete dropped items and chests

procedure generate_item(var items : c_items; i : integer);
var rng, mo_rng : integer;
begin
rng:=random(501);
case rng of
       0..99  : begin // ranged weapon
                  mo_rng:=random(maxGuns-1)+1;
                  items[i].name:=guns[mo_rng].name;
                  items[i].amount:=1;
                  items[i].tag:='G'
                end;
     100..199 : begin // melee weapon
                  mo_rng:=random(maxTools-1)+2;
                  items[i].name:=tools[mo_rng].name;
                  items[i].amount:=1;
                  items[i].tag:='T'
                end;
     200..299 : begin // ammo
                  mo_rng:=random(maxGuns)+1;
                  items[i].name:=ammo_types[mo_rng].name;
                  items[i].amount:=ammo_types[mo_rng].quantity;
                  items[i].tag:='B'
                end;
     300..399 : begin // orb of life
                  items[i].name:='Orb of life';
                  items[i].amount:=1;
                  items[i].tag:='L'
                end;
     400..499 : begin // orb of armor
                  items[i].name:='Orb of armor';
                  items[i].amount:=1;
                  items[i].tag:='A'
                end;
     500      : begin // get lucky
                  items[i].name:=guns[maxGuns].name;
                  items[i].amount:=1;
                  items[i].tag:='G'
                end
end
end; // generate item in a chest

procedure new_instance(var level : map; var rooms : c_rooms; var hero : player;
                       var monsters : c_monsters; var items : c_items; depth : byte);
var room_count, column_count : byte;
    exit_coords              : TCoord;
begin
void_map(level);
column_count:=generate_map(level, rooms, room_count);
// column_count:=build_map(level, 'glitch.log');
// column_count:=read_map(level, 'glitch.log');
// log_rooms(rooms, depth);
// log_map(level);
connect_rooms(level);
clear_items(items);
hero.prev_cell:=pass;
position_player(level, hero);
exit_coords:=position_exit(level, column_count);
if (depth=1) or (((depth mod 10)=0) and (depth<maxDepth)) then begin
  setlength(items, length(items)+1);
  if odd(column_count) then begin
    items[length(items)-1].x:=exit_coords.X+1;
    items[length(items)-1].y:=exit_coords.Y+1
  end else begin
    items[length(items)-1].x:=exit_coords.X+1;
    items[length(items)-1].y:=exit_coords.Y-1
  end;
  items[length(items)-1].name:='flashbang';
  items[length(items)-1].amount:=1;
  items[length(items)-1].tag:='F';
  level[items[length(items)-1].x, items[length(items)-1].y].ch:=chest
end;
position_chests(level, rooms, items);
position_monsters(level, rooms, monsters, hero, depth, room_count);
draw_map(level, rooms, hero);
// log_map(level);
// log_creatures(hero, monsters)
end; // generate new instance

procedure draw_frame(x1, y1, x2, y2 : byte; header, footer : string);
var x, y : byte;
begin
for y:=y1 to y2 do
  for x:=x1 to x2 do begin
    gotoxy(x, y); write(' ');
    if (x=x1) or  (x=x2) then begin gotoxy(x, y); write(chr(186)) end;
    if (y=y1) or  (y=y2) then begin gotoxy(x, y); write(chr(205)) end;
    if (x=x1) and (y=y1) then begin gotoxy(x, y); write(chr(201)) end;
    if (x=x2) and (y=y1) then begin gotoxy(x, y); write(chr(187)) end;
    if (x=x1) and (y=y2) then begin gotoxy(x, y); write(chr(200)) end;
    if (x=x2) and (y=y2) then begin gotoxy(x, y); write(chr(188)) end
  end;
if length(header)>0 then begin
  gotoxy((x1+(x2-x1) div 2)-(length(header) div 2)-1, y1); write(' ', header, ' ')
end;
if length(footer)>0 then begin
  gotoxy((x1+(x2-x1) div 2)-(length(header) div 2)-1, y2); write(' ', footer, ' ')
end
end; // draw frame

procedure clear_frame(x1, y1, x2, y2 : byte);
var frame_buffer : array [0..1999] of TCharInfo; // yes I'm retarded
    charPos      : TCoord;
    charBufSize  : TCoord;
    conWriteArea : TSmallRect;
    x            : word;
begin
charPos.X:=x1; charPos.Y:=y1;
charBufSize.X:=x2+1; charBufSize.Y:=y2+1;
conWriteArea.Left:=x1; conWriteArea.Top:=y1;
conWriteArea.Right:=x2; conWriteArea.Bottom:=y2;
// setlength(frame_buffer, (x2-x1)*(y2-y1));
for x:=0 to 1999 do begin
  frame_buffer[x].AsciiChar:=void;
  frame_buffer[x].Attributes:=0
end;
WriteConsoleOutputA(ConHandle, @frame_buffer, charBufSize, charPos, conWriteArea)
end; // clear insides of the frame

procedure list_items(items : c_items);
var i : byte;
begin
if length(items)<1 then begin
  gotoxy(7, 4); write('Nothing here.');
  exit
end;
if length(items)<14 then
  for i:=0 to length(items)-1 do begin
    gotoxy(7, 4+i);
    if (items[i].tag='B') or (items[i].tag='F') then begin
      SetConsoleTextAttribute(ConHandle, YELLOW);
      write(chr(97+i)); gotoxy(8, 4+i);
      SetConsoleTextAttribute(ConHandle, WHITE);
      write(' - ', items[i].name, ' (', items[i].amount, ')')
    end else begin
      SetConsoleTextAttribute(ConHandle, YELLOW);
      write(chr(97+i)); gotoxy(8, 4+i);
      SetConsoleTextAttribute(ConHandle, WHITE);
      write(' - ', items[i].name)
    end
  end
else begin
  for i:=0 to 12 do begin
    gotoxy(7, 4+i);
    if (items[i].tag='B') or (items[i].tag='F') then begin
      SetConsoleTextAttribute(ConHandle, YELLOW);
      write(chr(97+i)); gotoxy(8, 4+i);
      SetConsoleTextAttribute(ConHandle, WHITE);
      write(' - ', items[i].name, ' (', items[i].amount, ')')
    end else begin
      SetConsoleTextAttribute(ConHandle, YELLOW);
      write(chr(97+i)); gotoxy(8, 4+i);
      SetConsoleTextAttribute(ConHandle, WHITE);
      write(' - ', items[i].name)
    end
  end;
  for i:=13 to length(items)-1 do begin
    gotoxy(40, 4+(i-13));
    if (items[i].tag='B') or (items[i].tag='F') then begin
      SetConsoleTextAttribute(ConHandle, YELLOW);
      write(chr(110+(i-13))); gotoxy(41, 4+(i-13));
      SetConsoleTextAttribute(ConHandle, WHITE);
      write(' - ', items[i].name, ' (', items[i].amount, ')')
    end else begin
      SetConsoleTextAttribute(ConHandle, YELLOW);
      write(chr(110+(i-13))); gotoxy(41, 4+(i-13));
      SetConsoleTextAttribute(ConHandle, WHITE);
      write(' - ', items[i].name)
    end
  end
end
end; // list items in frame

procedure remove_item(var items : c_items; index : integer);
var i : integer;
begin
for i:=index to length(items)-2 do items[i]:=items[i+1];
setlength(items, length(items)-1)
end; // remove item from array

procedure show_item_info(name : string; damage, modifier : integer);
var keycode : word;
begin
SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
draw_frame(22, 8, 57, 12, 'Item info', '');
gotoxy(24, 9); write(capitalize(name));
gotoxy(24, 11);
if damage>0 then
  if modifier<>0 then write('Damage: ', damage-modifier, '-', damage+modifier)
                 else write('Damage: ', damage);
if damage=0 then write('Blind enemies in room (', blindDuration, ' turns)');
SetConsoleTextAttribute(ConHandle, WHITE);
while true do begin
  keycode:=readkey;
  if keycode=VK_ESCAPE then exit
end
end; // draw frame with weapon info

function show_inventory(var items : c_items; var hero : player;
                        var flash_bang : boolean) : boolean;
var i, j, q    : integer;
    mine       : c_items;
    pressed    : boolean;
    pressed_in : boolean;
    found      : boolean;
    keycode    : word;
    ltr        : char;
    amnt, a    : string;
    ammo_name  : string;
    swap_ammo  : string;
    swap       : item;
begin
draw_frame(5, 3, 75, 17, 'Inventory', '');
q:=0; result:=false;
for i:=0 to length(items)-1 do
  if items[i].x=0 then begin
    inc(q);
    setlength(mine, q);
    mine[q-1].x:=0;
    mine[q-1].y:=0;
    mine[q-1].name:=items[i].name;
    mine[q-1].amount:=items[i].amount;
    mine[q-1].tag:=items[i].tag
  end;
pressed:=false;
list_items(mine);
while not pressed do begin
  keycode:=readkey;
  if keycode=VK_ESCAPE then exit;
  if keycode in [$41..($41+length(mine)-1)] then begin
    ltr:=chr(97+keycode-$41);
    if (mine[keycode-$41].tag<>'G') and (mine[keycode-$41].tag<>'T') and
       (mine[keycode-$41].tag<>'F') then begin
      str(mine[keycode-$41].amount, amnt);
      SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
      draw_frame(22, 8, 57, 10, ltr+' - '+mine[keycode-$41].name+' ('+amnt+')', '');
      SetConsoleTextAttribute(ConHandle, YELLOW);
      gotoxy(24, 9); write('d');
      SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
      write('rop'); SetConsoleTextAttribute(ConHandle, WHITE)
    end else begin
      SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
      if mine[keycode-$41].tag='F' then begin
        str(mine[keycode-$41].amount, amnt);
        draw_frame(22, 8, 57, 12, ltr+' - '+mine[keycode-$41].name+' ('+amnt+')', '');
        SetConsoleTextAttribute(ConHandle, YELLOW);
        gotoxy(24, 9); write('u');
        SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
        write('se')
      end else begin
        draw_frame(22, 8, 57, 12, ltr+' - '+mine[keycode-$41].name, '');
        SetConsoleTextAttribute(ConHandle, YELLOW);
        gotoxy(24, 9); write('e');
        SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
        write('quip')
      end;
      SetConsoleTextAttribute(ConHandle, YELLOW);
      gotoxy(24, 10); write('d');
      SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
      write('rop'); SetConsoleTextAttribute(ConHandle, YELLOW);
      gotoxy(24, 11); write('i');
      SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
      write('nfo'); SetConsoleTextAttribute(ConHandle, WHITE)
    end;
    swap:=mine[keycode-$41];
    q:=keycode-$41; pressed_in:=false;
    while not pressed_in do begin
      keycode:=readkey;
      if keycode=VK_ESCAPE then pressed_in:=true;
      if keycode=$45 then begin // E = equip item
        if (swap.tag='F') or (swap.tag='B') then continue;
        result:=true;
        if swap.tag='T' then // melee weapon
          for i:=0 to maxTools do
            if tools[i].name=swap.name then begin
              if hero.tool.name=tools[1].name then begin
                remove_item(mine, q);
                for j:=0 to length(items)-1 do
                  if (items[j].name=swap.name) and (items[j].x=0) then begin
                    remove_item(items, j);
                    break
                  end
              end else begin
                mine[q].name:=hero.tool.name;
                for j:=0 to length(items)-1 do
                  if (items[j].name=swap.name) and (items[j].x=0) then begin
                    items[j].name:=hero.tool.name;
                    break
                  end
              end;
              hero.tool:=tools[i];
              gotoxy(30, 22); write(capitalize(hero.tool.name), ' equipped.');
              break
            end;
        if swap.tag='G' then begin // firearm
          // swap weapon
          mine[q].name:=hero.firearm.name;
          for i:=0 to maxGuns do
            if guns[i].name=swap.name then begin
              hero.firearm:=guns[i];
              gotoxy(30, 22); write(capitalize(hero.firearm.name), ' equipped.');
              for j:=0 to length(items)-1 do
                if items[j].name=swap.name then begin
                  items[j].name:=mine[q].name;
                  break
                end;
              break
            end;
          // swap ammo
          for i:=0 to maxGuns do begin
            if ammo_types[i].weapon=swap.name then swap_ammo:=ammo_types[i].name;
            if ammo_types[i].weapon=mine[q].name then ammo_name:=ammo_types[i].name
          end;
          found:=false;
          for i:=0 to length(mine)-1 do
            if mine[i].name=swap_ammo then begin
              swap:=mine[i];
              mine[i].name:=ammo_name;
              mine[i].amount:=hero.ammo;
              for j:=0 to length(items)-1 do
                if items[j].name=swap_ammo then begin
                  items[j].name:=mine[i].name;
                  items[j].amount:=mine[i].amount;
                  if hero.ammo<1 then remove_item(items, j);
                  found:=true; break
                end;
              break
            end;
          if (not found) and (hero.ammo>1) then begin
            setlength(items, length(items)+1);
            items[length(items)-1].x:=0;
            items[length(items)-1].y:=0;
            items[length(items)-1].name:=ammo_name;
            items[length(items)-1].amount:=hero.ammo;
            items[length(items)-1].tag:='B';
            hero.ammo:=0
          end;
          if found then hero.ammo:=swap.amount
        end; // firearm
        clear_frame(0, 22, 29, 24);
        pressed:=true; pressed_in:=true
      end; // equip item
      if keycode=$55 then begin // U = use item
        if (swap.tag='G') or (swap.tag='T') then continue;
        if swap.tag='F' then begin // flashbang
          flash_bang:=true;
          for i:=0 to length(items)-1 do
            if items[i].name=swap.name then begin
              dec(items[i].amount);
              if items[i].amount<=0 then remove_item(items, i);
              break
            end
        end;
        clear_frame(0, 22, 29, 24);
        pressed:=true; pressed_in:=true
      end; // use item
      if keycode=$44 then begin // D = drop item
        result:=true;
        mine[q].x:=hero.x;
        mine[q].y:=hero.y;
        for i:=0 to length(items)-1 do
          if (items[i].name=mine[q].name) and (items[i].x=0) then begin
            items[i].x:=hero.x;
            items[i].y:=hero.y;
            break
          end;
        if hero.prev_cell<>corpse then hero.prev_cell:=clip;
        gotoxy(30, 22);
        a:=mine[q].name;
        if (mine[q].tag='B') or (mine[q].tag='F') then begin
          if (mine[q].amount=1) and (mine[q].tag='B') then
            a:=copy(a, 1, length(a)-1);
          if (mine[q].amount>1) and (mine[q].tag='F') then a:=a+'s';
          write('Dropped ', mine[q].amount, ' ', a, '.')
        end else begin
          if items[q].name[1]='a' then a:='an' else a:='a';
          write('Dropped ', a, ' ', mine[q].name, '.')
        end;
        pressed:=true; pressed_in:=true
      end; // drop item
      if (keycode=$49) and (swap.tag<>'B') then begin // I = item info
        if swap.tag='T' then
          for i:=1 to maxTools do
            if tools[i].name=swap.name then begin
              show_item_info(tools[i].name, tools[i].damage, tools[i].modifier);
              break
            end;
        if swap.tag='G' then
          for i:=1 to maxGuns do
            if guns[i].name=swap.name then begin
              show_item_info(guns[i].name, guns[i].damage, guns[i].modifier);
              break
            end;
        if swap.tag='F' then show_item_info('flashbang', 0, 0);
        pressed_in:=true
      end; // examine item
      if pressed_in and not pressed then begin
        clear_frame(6, 4, 74, 16);
        list_items(mine)
      end
    end // 'e', 'd' or 'i' pressed
  end // interact with item
end // key pressed
end; // inventory dialog

procedure clear_messages;
begin
clear_frame(30, 22, 70, 24)
end; // clear system messages

procedure display_messages(msg : string);
  procedure show_msg_array;
  var i : byte;
  begin
  for i:=0 to 2 do begin
    gotoxy(30, 22+i);
    if pos('"', msg_array[i])<>0 then
      SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
    write(msg_array[i]); SetConsoleTextAttribute(ConHandle, WHITE)
  end
  end; // show msg_array
var i : byte;
begin
for i:=0 to 2 do
  if msg_array[i]='' then begin
    msg_array[i]:=msg;
    show_msg_array;
    exit
  end;
for i:=1 to 2 do
  if msg_array[i]<>'' then msg_array[i-1]:=msg_array[i];
if msg_array[2]<>'' then msg_array[2]:=msg;
clear_messages;
show_msg_array
end; // display messages array

procedure clean_up_items(var items : c_items; x, y : shortint);
var i : integer;
begin
for i:=0 to length(items)-1 do
  if (items[i].x=x) and (items[i].y=y) and
     (items[i].name='empty') then remove_item(items, i)
end; // clean up the spot

procedure auto_pickup(var items : c_items; x, y : shortint; var hero : player);
var i                 : integer;
    medkit_value      : integer;
    armor_shard_value : integer;
begin
for i:=1 to maxMiscStuff do begin
  if misc_stuff[i].name='medkit'      then medkit_value:=misc_stuff[i].value;
  if misc_stuff[i].name='armor shard' then armor_shard_value:=misc_stuff[i].value
end;
for i:=0 to length(items) do
  if (items[i].x=x) and (items[i].y=y) and (items[i].tag='M') and
     (items[i].name<>'empty') then begin
    if items[i].name='medkit' then begin
      hero.life:=hero.life+medkit_value;
      items[i].name:='empty';
      display_messages('Picked up a medkit (+'+inttostr(medkit_value)+' life)')
    end;
    if items[i].name='armor shard' then begin
      hero.armor:=hero.armor+armor_shard_value;
      items[i].name:='empty';
      display_messages('Picked up an armor shard (+'+inttostr(armor_shard_value)+' armor)')
    end;
    if items[i].name='keycard' then begin
      items[i].x:=0; items[i].y:=0;
      display_messages('Picked up a keycard.')
    end
  end; // tag M, not empty
clean_up_items(items, x, y)
end; // auto-pickup small stuff

procedure pick_up_chest(var items : c_items; i : integer; var hero : player);
var j, k          : integer;
    found         : boolean;
    ammo_name, a  : string;
    weapon_name   : string;
    ammo_quantity : byte;
    my_items      : byte;
begin
my_items:=0;
for j:=0 to length(items)-1 do
  if (items[j].x=0) and (items[j].name<>'empty') then inc(my_items);
if my_items>=26 then begin
  gotoxy(30, 22); write('Can''t carry any more.');
  exit
end;
found:=false;
if items[i].tag='G' then begin // it's a gun
  for k:=1 to maxGuns do
    if items[i].name=ammo_types[k].weapon then begin
      ammo_name:=ammo_types[k].name;
      ammo_quantity:=ammo_types[k].quantity;
      break
    end;
  if hero.firearm.name=items[i].name then begin // and I have it equipped
    hero.ammo:=hero.ammo+ammo_quantity;
    found:=true
  end;
  if not found then// let's see
    for j:=0 to length(items)-1 do // do I have it?
      if (items[j].name=items[i].name) and
         (items[j].x=0) then begin // yes, I do
        for k:=0 to length(items)-1 do
          if (items[k].name=ammo_name) and
             (items[k].x=0) then begin
            items[k].amount:=items[k].amount+ammo_quantity;
            found:=true;
            break
          end;
        if not found then begin // no ammo for it though
          setlength(items, length(items)+1);
          items[length(items)-1].x:=0;
          items[length(items)-1].y:=0;
          items[length(items)-1].name:=ammo_name;
          items[length(items)-1].amount:=ammo_quantity;
          items[length(items)-1].tag:='B';
          found:=true
        end;
        break
      end;
  if found then begin
    clear_messages;
    gotoxy(30, 22); write('Extracted ', ammo_quantity, ' ', ammo_name, '.')
  end;
  if not found then begin // I don't have it
    setlength(items, length(items)+1);
    items[length(items)-1].x:=0;
    items[length(items)-1].y:=0;
    items[length(items)-1].name:=items[i].name;
    items[length(items)-1].amount:=1;
    items[length(items)-1].tag:='G';
    clear_messages;
    if items[i].name[1]='a' then a:='an ' else a:='a ';
    gotoxy(30, 22); write('Acquired ', a, items[i].name, '.');
    for j:=0 to length(items)-1 do
      if (items[j].name=ammo_name) and (items[j].x=0) then begin
        items[j].amount:=items[j].amount+ammo_quantity; // I have the ammo though
        found:=true; break
      end;
    if not found then begin // don't have the ammo either
      setlength(items, length(items)+1);
      items[length(items)-1].x:=0;
      items[length(items)-1].y:=0;
      items[length(items)-1].name:=ammo_name;
      items[length(items)-1].amount:=ammo_quantity;
      items[length(items)-1].tag:='B'
    end;
    gotoxy(30, 23); write('It''s loaded with ', ammo_quantity,
                          ' ', ammo_name, '.')
  end
end; // tag G
if items[i].tag='B' then begin // some ammo
  for j:=1 to maxGuns do
    if items[i].name=ammo_types[j].name then begin
      weapon_name:=ammo_types[j].weapon;
      break
    end;
  if hero.firearm.name=weapon_name then begin // ammo for equipped weapon
    hero.ammo:=hero.ammo+items[i].amount;
    found:=true
  end;
  if not found then // check inventory
    for j:=0 to length(items)-1 do
      if (items[j].name=items[i].name) and
         (items[j].x=0) then begin // got it
        items[j].amount:=items[j].amount+items[i].amount;
        found:=true;
        break
      end;
  if not found then begin
    setlength(items, length(items)+1);
    items[length(items)-1].x:=0;
    items[length(items)-1].y:=0;
    items[length(items)-1].name:=items[i].name;
    items[length(items)-1].amount:=items[i].amount;
    items[length(items)-1].tag:='B'
  end;
  clear_messages;
  gotoxy(30, 22); write('Acquired ', items[i].amount, ' ', items[i].name, '.')
end; // tag B
if items[i].tag='T' then begin // melee weapon
  setlength(items, length(items)+1);
  items[length(items)-1].x:=0;
  items[length(items)-1].y:=0;
  items[length(items)-1].name:=items[i].name;
  items[length(items)-1].amount:=1;
  items[length(items)-1].tag:='T';
  clear_messages;
  if items[i].name[1]='a' then a:='an ' else a:='a ';
  gotoxy(30, 22); write('Acquired ', a, items[i].name, '.')
end; // tag T
if items[i].tag='F' then begin // flashbang
  for j:=0 to length(items)-1 do
    if (items[j].name='flashbang') and (items[j].x=0) then begin // have some
      items[j].amount:=items[j].amount+items[i].amount;
      found:=true;
      break
    end;
  if not found then begin
    setlength(items, length(items)+1);
    items[length(items)-1].x:=0;
    items[length(items)-1].y:=0;
    items[length(items)-1].name:=items[i].name;
    items[length(items)-1].amount:=items[i].amount;
    items[length(items)-1].tag:='F'
  end;
  clear_messages;
  gotoxy(30, 22); write('Acquired ', items[i].amount, ' ', items[i].name, '.')
end; // tag F
if items[i].tag='L' then begin // life orb
  hero.life:=hero.life+orbOfLifeValue;
  clear_messages;
  gotoxy(30, 22); write('Gained ', orbOfLifeValue, ' life.')
end; // tag L
if items[i].tag='A' then begin // armor orb
  hero.armor:=hero.armor+orbOfArmorValue;
  clear_messages;
  gotoxy(30, 22); write('Gained ', orbOfArmorValue, ' armor.')
end; // tag A
end; // pick up item from chest

procedure pick_up_items(var items : c_items; var hero : player);
type mark_item = record
       props  : item;
       marked : boolean
end;
var ground        : array of mark_item;
    g_list        : c_items;
    i, j, q       : integer;
    pressed       : boolean;
    found         : boolean;
    keycode       : word;
    mx, my        : shortint;
    w_name        : string;
    a             : string;
begin
q:=0;
for i:=0 to length(items)-1 do
  if (items[i].x=hero.x) and (items[i].y=hero.y) and
     (items[i].name<>'empty') then begin
    inc(q); setlength(ground, q);
    ground[q-1].props:=items[i];
    ground[q-1].marked:=false;
    setlength(g_list, q);
    g_list[q-1]:=items[i]
  end;
list_items(g_list);
pressed:=false;
while not pressed do begin
  keycode:=readkey;
  if keycode=VK_ESCAPE then pressed:=true;
  if keycode in [$41..($41+length(ground)-1)] then begin
    ground[keycode-$41].marked:=not ground[keycode-$41].marked;
    if keycode in [$4e..$5a] then begin
      mx:=33; my:=-13
    end else begin
      mx:=0; my:=0
    end;
    gotoxy(9+mx, 4+keycode-$41+my);
    if ground[keycode-$41].marked then write('+') else write('-')
  end;
  if keycode=VK_RETURN then begin
    if length(g_list)>=26 then begin
      gotoxy(30, 22); write('Can''t carry any more.');
      exit
    end;
    for i:=0 to length(ground)-1 do
      if ground[i].marked then
        for q:=0 to length(items)-1 do
          if (ground[i].props.x=items[q].x) and
             (ground[i].props.y=items[q].y) and
             (ground[i].props.name=items[q].name) then begin
            case items[q].tag of
                 'T',
                 'G',
                 'M' : begin // gun or melee weapon, also keycard
                         items[q].x:=0;
                         items[q].y:=0;
                         if items[q].name[1]='a' then a:='an' else a:='a';
                         display_messages('Picked up '+a+' '+items[q].name+'.')
                       end; // tag T, G, M
                 'F' : begin // flashbang
                         found:=false;
                         for j:=0 to length(items)-1 do // search bag
                           if (items[j].name=items[q].name) and
                              (items[j].x=0) then begin
                             items[j].amount:=items[j].amount+items[q].amount;
                             found:=true; break
                           end;
                         if not found then begin // don't have
                           items[q].x:=0;
                           items[q].y:=0
                         end;
                         a:=items[q].name;
                         if items[q].amount>1 then a:=a+'s';
                         display_messages('Picked up '+inttostr(items[q].amount)+
                                          ' '+a+'.');
                         if found then items[q].name:='empty'
                       end; // tag F
                 'B' : begin // ammo
                         for j:=1 to maxGuns do
                           if items[q].name=ammo_types[j].name then begin
                             w_name:=ammo_types[j].weapon;
                             break
                           end;
                         found:=false;
                         if hero.firearm.name=w_name then begin  // is it for my
                           hero.ammo:=hero.ammo+items[q].amount; // equipped gun?
                           found:=true
                         end;
                         if not found then
                           for j:=0 to length(items)-1 do // do I have it in my bag?
                             if (items[j].name=items[q].name) and
                                (items[j].x=0) then begin
                               items[j].amount:=items[j].amount+items[q].amount;
                               found:=true; break
                             end;
                         if not found then begin // I don't have it
                           items[q].x:=0;
                           items[q].y:=0
                         end;
                         a:=items[q].name;
                         if items[q].amount=1 then a:=copy(a, 1, length(a)-1);
                         display_messages('Picked up '+inttostr(items[q].amount)+
                                          ' '+a+'.');
                         if found then items[q].name:='empty'
                       end // tag B
            end; // case
            break
          end; // items loop
    pressed:=true
  end; // enter hit
end; // readkey
clean_up_items(items, hero.x, hero.y)
end; // pick up items from the ground

function check_the_ground(var items : c_items; var hero : player) : boolean;
var pressed : boolean;
    keycode : word;
    i       : integer;
begin
gotoxy(30, 22); write('You see something on the ground.');
gotoxy(30, 23); write('Do you want to check it out? (');
SetConsoleTextAttribute(ConHandle, YELLOW); write('y');
SetConsoleTextAttribute(ConHandle, WHITE); write('/');
SetConsoleTextAttribute(ConHandle, YELLOW); write('n');
SetConsoleTextAttribute(ConHandle, WHITE); write(')');
pressed:=false; result:=false;
while not pressed do begin
  keycode:=readkey;
  case keycode of
       $59 : begin // Y
               clear_messages;
               draw_frame(5, 3, 75, 17, 'Select item(s) to pick up',
                                        'Enter = confirm selection');
               pick_up_items(items, hero);
               for i:=0 to length(items)-1 do
                 if (items[i].x=hero.x) and (items[i].y=hero.y) and
                    (items[i].name<>'empty') then begin
                   result:=true;
                   break
                 end;
               pressed:=true
             end;
       $4e : begin // N
               result:=true;
               clear_messages;
               pressed:=true
             end
  else continue
  end
end // readkey
end; // stuff on the ground

function do_damage(x, y : shortint; var monsters : c_monsters; var hero : player;
                   var items : c_items; damage_source : char) : boolean;
var mod_rng, mod_value : integer;
    damage_value       : integer;
    i, j               : integer;
begin
clear_messages; result:=false;
if (damage_source='r') and (hero.ammo=0) then begin
  gotoxy(30, 22); write('No ammo left.'); exit
end;
i:=-1;
for j:=0 to length(monsters)-1 do
  if (monsters[j].x=x) and (monsters[j].y=y) and (monsters[j].state=alive)
  then begin i:=j; break end;
mod_rng:=random(100)+1;
case damage_source of
     'm' : begin // hit with melee weapon
             mod_value:=random(hero.tool.modifier+1);
             if mod_rng>50 then
               damage_value:=hero.tool.damage+mod_value
             else
               damage_value:=hero.tool.damage-mod_value
           end;
     'r' : begin // firearm
             mod_value:=random(hero.firearm.modifier+1);
             if mod_rng>50 then
               damage_value:=hero.firearm.damage+mod_value
             else
               damage_value:=hero.firearm.damage-mod_value
           end
end;
if damage_source='r' then dec(hero.ammo);
if (x=hero.x) and (y=hero.y) then begin
  damage_value:=damage_value-(hero.armor div arMitigation);
  display_messages('You deal '+inttostr(damage_value)+' damage to yourself.');
  hero.life:=hero.life-damage_value;
  if hero.life<=0 then display_messages('You die...');
  exit
end;
if i<0 then begin
  display_messages('Waste of ammo.');
  exit
end;
damage_value:=damage_value-(monsters[i].armor div arMitigation);
monsters[i].life:=monsters[i].life-damage_value;
display_messages('You deal '+inttostr(damage_value)+' damage to '+monsters[i].name+'.');
SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_INTENSITY);
gotoxy(hero.x, hero.y); write(hero.face); sleep(fxDelay);
SetConsoleTextAttribute(ConHandle, WHITE);
monsters[i].pursuit:=true;
if monsters[i].life<=0 then begin
  monsters[i].face:=corpse;
  monsters[i].state:=dead;
  display_messages(capitalize(monsters[i].name)+' dies.');
  setlength(items, length(items)+1);
  mod_rng:=random(maxMiscStuff)+1;
  items[length(items)-1].x:=monsters[i].x;
  items[length(items)-1].y:=monsters[i].y;
  items[length(items)-1].name:=misc_stuff[mod_rng].name;
  items[length(items)-1].amount:=misc_stuff[mod_rng].value;
  items[length(items)-1].tag:='M';
  if monsters[i].name=monster_types[maxMonsters].name then begin
    setlength(items, length(items)+1);
    items[length(items)-1].x:=monsters[i].x;
    items[length(items)-1].y:=monsters[i].y;
    items[length(items)-1].name:='keycard';
    items[length(items)-1].amount:=1;
    items[length(items)-1].tag:='M'
  end;
  result:=true
end
end; // deal damage to a monster

function monster_damage(a_monster, hero : player; mode : char) : integer;
var i, damage_value    : integer;
    mod_rng, mod_value : integer;
    damage, modifier   : integer;
begin
case mode of
     'r' : // ranged attack
           for i:=1 to length(guns) do
             if a_monster.firearm.name=guns[i].name then begin
               damage:=guns[i].damage;
               modifier:=guns[i].modifier;
               break
             end;
     'm' : // melee attack
           for i:=1 to length(tools) do
             if a_monster.tool.name=tools[i].name then begin
               damage:=tools[i].damage;
               modifier:=tools[i].modifier;
               break
             end
end;
mod_rng:=random(100)+1;
mod_value:=random(modifier+1);
if mod_rng<50 then damage_value:=damage-mod_value
              else damage_value:=damage+mod_value;
result:=damage_value-(hero.armor div arMitigation)
end; // return monster damage value

procedure inspect(x, y : byte; level : map; monsters : c_monsters; hero : player);
var i : integer;
    a : string;
begin
clear_messages;
gotoxy(30, 22);
if not level[x, y].revealed then begin
  write('Unrevealed spot.'); exit
end;
if level[x, y].ch=wall      then write('It''s a wall.');
if level[x, y].ch=void      then write('Who knows what''s out there.');
if level[x, y].ch=pass      then write('It''s a floor.');
if level[x, y].ch=door      then write('It''s a door.');
if level[x, y].ch=corpse    then write('A dead body.');
if level[x, y].ch=chest     then write('A treasure chest!');
if level[x, y].ch=clip      then write('Something you dropped earlier.');
if level[x, y].ch=hero.face then write(hero.name, '. Loaded with power.');
if level[x, y].ch in ['a'..'z', 'A'..'Z'] then
  for i:=1 to maxMonsters do
    if level[x, y].ch=monster_types[i].face then begin
      gotoxy(30, 22); write(capitalize(monster_types[i].name), '.');
      if monster_types[i].weapon[1]='a' then a:='an' else a:='a';
      gotoxy(30, 23); write('Armed with ', a, ' ', monster_types[i].weapon, '.');
      break
    end
end; // inspect a cell

procedure highlight(level : map; x, y : shortint);
var background : byte;
    foreground : byte;
begin
background:=BACKGROUND_BLUE or BACKGROUND_INTENSITY;
foreground:=FOREGROUND_INTENSITY;
SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_GREEN or
                                   FOREGROUND_BLUE or foreground or
                                   background);
if level[x, y].ch=chest then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_GREEN or
                                     foreground or background);
if level[x, y].ch=corpse then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or foreground or
                                     background);
if level[x, y].ch=clip then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_BLUE or FOREGROUND_GREEN or
                                     foreground or background);
if level[x, y].ch=monster_types[maxMonsters].face then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_BLUE or
                                     foreground or background);
gotoxy(x, y); if level[x, y].revealed then write(level[x, y].ch)
                                      else write(void);
SetConsoleTextAttribute(ConHandle, WHITE); gotoxy(x, y)
end; // highlight a cell

function connect_xy(x1, y1, x2, y2 : shortint; var level : map;
                    var monsters : c_monsters; var hero : player;
                    var items : c_items; mode : char;
                    var next_x, next_y : shortint) : boolean;
var deltaX, deltaY : byte;
    signX, signY   : shortint;
    error, error2  : smallint;
begin
result:=true;
deltaX:=abs(x2-x1);
deltaY:=abs(y2-y1);
if x1<x2 then signX:=1 else signX:=-1;
if y1<y2 then signY:=1 else signY:=-1;
error:=deltaX-deltaY;
while (x1<>x2) or (y1<>y2) do begin
  if mode='h' then highlight(level, x1, y1);
  error2:=error*2;
  if error2>-deltaY then begin
    error:=error-deltaY;
    x1:=x1+signX
  end;
  if error2<deltaX then begin
    error:=error+deltaX;
    y1:=y1+signY
  end;
  if (mode='a') and ((level[x1, y1].ch=wall) or (level[x1, y1].ch=door) or
                     (level[x1, y1].ch in ['a'..'z', 'A'..'Z'])) then begin
    if do_damage(x1, y1, monsters, hero, items, 'r') then level[x1, y1].ch:=corpse;
    exit
  end;
  if (mode='c') and ((level[x1, y1].ch=wall) or (level[x1, y1].ch=door) or
                     (level[x1, y1].ch in ['a'..'z', 'A'..'Z'])) then begin
    result:=false; exit
  end;
  if (mode='m') and ((level[x1, y1].ch=wall) or (level[x1, y1].ch=door) or
                     (level[x1, y1].ch in ['a'..'z', 'A'..'Z']) or
                     (level[x1, y1].ch=chest)) then begin
    result:=false; exit
  end;
  if mode='p' then begin
    next_x:=x1; next_y:=y1; exit
  end
end;
if mode='a' then do_damage(x2, y2, monsters, hero, items, 'r');
if mode='h' then highlight(level, x2, y2)
end; // connect two spots in map (draw a line)

function tile_info(x, y : shortint; level : map; hero : player) : string;
var i : integer;
begin
result:='wtf';
if level[x, y].ch=wall then result:='wall';
if level[x, y].ch=void then result:='unrevealed spot';
if level[x, y].ch=pass then result:='floor';
if level[x, y].ch=door then result:='door';
if level[x, y].ch=corpse then result:='dead body';
if level[x, y].ch=chest then result:='chest';
if level[x, y].ch=clip then result:='item pile';
if level[x, y].ch=hero.face then result:='yourself';
if level[x, y].ch in ['a'..'z', 'A'..'Z'] then
  for i:=1 to maxMonsters do
    if level[x, y].ch=monster_types[i].face then begin
      result:=monster_types[i].name;
      break
    end;
if not level[x, y].revealed then result:='unrevealed spot'
end; // return basic tile info

function select_target(var level : map; rooms : c_rooms; var monsters : c_monsters;
                       var hero : player; var items : c_items; mode : char) : boolean;
var keycode  : word;
    mx, my   : shortint;
    dx, dy   : shortint;
    sorted   : c_monsters;
    swap     : player;
    i, j, k  : integer;
    locked   : boolean;
    st       : string;
begin
dx:=0; dy:=0; result:=true;
gotoxy(30, 22);
case mode of
     'f' : begin
             write('Select a target to shoot.');
             j:=0;
             for i:=0 to length(monsters)-1 do
               if monsters[i].state=alive then begin
                  inc(j); setlength(sorted, j);
                  sorted[j-1]:=monsters[i]
               end;
             if length(sorted)>1 then
               for i:=0 to length(sorted)-1 do
                 for j:=0 to length(sorted)-2 do
                   if (abs(sorted[j].x-hero.x)+abs(sorted[j].y-hero.y))>
                      (abs(sorted[j+1].x-hero.x)+abs(sorted[j+1].y-hero.y))
                   then begin swap:=sorted[j];
                              sorted[j]:=sorted[j+1];
                              sorted[j+1]:=swap end;
             locked:=false;
             if length(sorted)>0 then
               for i:=0 to length(sorted)-1 do
                 if connect_xy(sorted[i].x, sorted[i].y, hero.x, hero.y, level,
                               sorted, hero, items, 'c', mx, my) and
                    level[sorted[i].x, sorted[i].y].revealed then begin
                   locked:=true;
                   dx:=sorted[i].x-hero.x;
                   dy:=sorted[i].y-hero.y;
                   connect_xy(hero.x, hero.y, hero.x+dx, hero.y+dy, level,
                              monsters, hero, items, 'h', mx, my);
                   break
                 end;
             if not locked then begin
               highlight(level, hero.x, hero.y);
               gotoxy(hero.x, hero.y)
             end
           end;
     'i' : begin
             write('Select a target to examine.');
             highlight(level, hero.x, hero.y);
             gotoxy(hero.x, hero.y)
           end
end;
mx:=0; my:=0;
while true do begin
  st:=tile_info(hero.x+dx, hero.y+dy, level, hero);
  clear_frame(30, 23, 70, 23);
  gotoxy(30, 23); write('(', st, ')'); gotoxy(hero.x+dx, hero.y+dy);
  keycode:=readkey;
  case keycode of
       VK_LEFT,   $64 : mx:=mx-1; // left
       VK_RIGHT,  $66 : mx:=mx+1; // right
       VK_UP,     $68 : my:=my-1; // up
       VK_DOWN,   $62 : my:=my+1; // down
       VK_PRIOR,  $69 : begin mx:=mx+1; my:=my-1 end; // up+right
       VK_NEXT,   $63 : begin mx:=mx+1; my:=my+1 end; // down+right
       VK_HOME,   $67 : begin mx:=mx-1; my:=my-1 end; // up+left
       VK_END,    $61 : begin mx:=mx-1; my:=my+1 end; // down+left
       VK_TAB         : if mode='f' then begin // switch target
                          for i:=0 to length(sorted)-1 do
                            if (sorted[i].x=hero.x+dx) and
                               (sorted[i].y=hero.y+dy) then begin j:=i; break end;
                          if j=(length(sorted)-1) then j:=0 else j:=j+1;
                          locked:=false;
                          for i:=j to length(sorted)-1 do
                            if (connect_xy(sorted[i].x, sorted[i].y,
                                           hero.x, hero.y, level, sorted, hero,
                                           items, 'c', mx, my)) and
                               level[sorted[i].x, sorted[i].y].revealed
                            then begin
                              k:=i; locked:=true;
                              break
                            end;
                          if not locked then
                            for i:=0 to length(sorted)-1 do
                              if (connect_xy(sorted[i].x, sorted[i].y,
                                             hero.x, hero.y, level, sorted, hero,
                                             items, 'c', mx, my)) and
                                 level[sorted[i].x, sorted[i].y].revealed
                              then begin
                                k:=i; locked:=true;
                                break
                              end;
                          if locked then begin
                            dx:=sorted[k].x-hero.x;
                            dy:=sorted[k].y-hero.y
                          end;
                          mx:=0; my:=0
                        end;
       VK_RETURN, $46 : begin // confirmed
                          case mode of
                            'f' : connect_xy(hero.x, hero.y, hero.x+dx, hero.y+dy,
                                             level, monsters, hero, items, 'a', mx, my);
                            'i' : inspect(hero.x+dx, hero.y+dy, level,
                                          monsters, hero)
                          end;
                          break
                        end;
       VK_ESCAPE      : begin clear_messages; result:=false; break end
  else continue
  end;
  draw_map(level, rooms, hero);
  if ((hero.x+dx+mx)<80) and ((hero.x+dx+mx)>-1) then dx:=dx+mx;
  if ((hero.y+dy+my)<22) and ((hero.y+dy+my)>-1) then dy:=dy+my;
  if mode='f' then connect_xy(hero.x, hero.y, hero.x+dx, hero.y+dy,
                              level, monsters, hero, items, 'h', mx, my);
  if mode='i' then begin
    highlight(level, hero.x+dx, hero.y+dy);
    gotoxy(hero.x+dx, hero.y+dy)
  end;
  mx:=0; my:=0
end // readkey
end; // select target to inspect or kill

procedure pathfind(x1, y1, x2, y2 : shortint; level : map;
                   var next_x, next_y : shortint);
  function in_queue(x, y : shortint; queue : c_queue) : boolean;
  var i : integer;
  begin
  result:=false;
  for i:=0 to length(queue)-1 do
    if (queue[i, 0]=x) and (queue[i, 1]=y) then begin
      result:=true; break
    end
  end; // tile is already in queue
var x, y      : shortint;
    queue     : c_queue;
    i, j, k   : integer;
    steps     : integer;
    queue_len : integer;
    done      : boolean;
    added     : boolean;
    stuck     : boolean;
    re_init   : boolean;
begin
setlength(queue, 1, 3);
queue[0, 0]:=x2; queue[0, 1]:=y2; queue[0, 2]:=0;
done:=false; stuck:=false; re_init:=false; steps:=0;
while not done do begin
  queue_len:=length(queue)-1;
  added:=false;
  for i:=0 to queue_len do begin
    if queue[i, 2]<steps then continue;
    x:=queue[i, 0]; y:=queue[i, 1];
    for j:=y-1 to y+1 do
      for k:=x-1 to x+1 do begin
        if (k=x1) and (j=y1) then begin done:=true; break end;
        if not stuck and (((j=y) and (k=x)) or (level[k, j].ch=wall) or
                          (level[k, j].ch=chest) or (level[k, j].ch=door) or
                          (level[k, j].ch in ['a'..'z', 'A'..'Z']) or
                           in_queue(k, j, queue)) then continue;
        if stuck and (((j=y) and (k=x)) or (level[k, j].ch=wall) or
                      (level[k, j].ch=chest) or (level[k, j].ch=door) or
                       in_queue(k, j, queue)) then continue;
        added:=true;
        setlength(queue, length(queue)+1, 3);
        queue[length(queue)-1, 0]:=k;
        queue[length(queue)-1, 1]:=j;
        queue[length(queue)-1, 2]:=steps+1
      end; // scan adjacent tiles
     if done then break
  end; // queue scan
  if (not added) and (not done) then stuck:=true;
  if stuck and not re_init then begin
    re_init:=true; steps:=-1;
    setlength(queue, 1, 3);
    queue[0, 0]:=x2; queue[0, 1]:=y2; queue[0, 2]:=0
  end;
  process_messages;
  inc(steps)
end; // build pathing sequence
next_x:=x1; next_y:=y1;
for i:=0 to length(queue)-1 do
  if (queue[i, 2]=steps-1) and
     (queue[i, 0] in [x1-1..x1+1]) and
     (queue[i, 1] in [y1-1..y1+1]) then begin
    next_x:=queue[i, 0];
    next_y:=queue[i, 1];
    break
  end
end; // pathfinding yo (slow as fuck)

function move_player(var level : map; var rooms : c_rooms; var hero : player;
                     var items : c_items; var monsters : c_monsters;
                     var depth : byte; direction : byte) : boolean;
var mx, my        : shortint;
    i, j          : integer;
    keycode       : word;
    pressed       : boolean;
begin
mx:=0; my:=0; result:=false;
case direction of
     4 : mx:=mx-1; // left
     6 : mx:=mx+1; // right
     8 : my:=my-1; // up
     2 : my:=my+1; // down
     1 : begin mx:=mx-1; my:=my+1 end; // left+down
     7 : begin mx:=mx-1; my:=my-1 end; // left+up
     3 : begin mx:=mx+1; my:=my+1 end; // right+down
     9 : begin mx:=mx+1; my:=my-1 end; // right+up
end;
if level[hero.x+mx, hero.y+my].ch=pass then begin
  level[hero.x, hero.y].ch:=hero.prev_cell;
  hero.x:=hero.x+mx;
  hero.y:=hero.y+my;
  hero.prev_cell:=pass;
  level[hero.x, hero.y].ch:=hero.face;
  draw_map(level, rooms, hero);
  exit
end; // pass
if level[hero.x+mx, hero.y+my].ch=door then begin
  if depth=maxDepth then begin
    for i:=0 to length(items)-1 do
      if (items[i].name='keycard') and (items[i].x=0) then begin // win condition
        gotoxy(30, 22); write('You have escaped!');
        result:=true; exit
      end;
    gotoxy(30, 22); write('The door is locked.');
    exit
  end;
  inc(depth);
  new_instance(level, rooms, hero, monsters, items, depth);
  exit
end; // door
if level[hero.x+mx, hero.y+my].ch=chest then begin
  gotoxy(30, 22); write('There is a chest.');
  for j:=0 to length(items)-1 do
    if (items[j].x=hero.x+mx) and (items[j].y=hero.y+my) then begin
      i:=j; break
    end;
  if items[i].name='empty' then begin
    gotoxy(30, 23); write('It''s empty.');
    exit
  end;
  if items[i].name='unknown' then generate_item(items, i);
  gotoxy(30, 23); write('You see ', items[i].amount, ' ', items[i].name, ' inside.');
  gotoxy(30, 24); write('Would you like to pick that up? (');
  SetConsoleTextAttribute(ConHandle, YELLOW); write('y');
  SetConsoleTextAttribute(ConHandle, WHITE); write('/');
  SetConsoleTextAttribute(ConHandle, YELLOW); write('n');
  SetConsoleTextAttribute(ConHandle, WHITE); write(')');
  pressed:=false;
  while not pressed do begin
    keycode:=readkey;
    case keycode of
         $59 : begin // Y
                 pick_up_chest(items, i, hero);
                 items[i].name:='empty';
                 pressed:=true
               end;
         $4e : begin // N
                 clear_messages;
                 pressed:=true
               end
    else continue
    end
  end; // readkey
  exit
end; // chest
if level[hero.x+mx, hero.y+my].ch=clip then begin
  level[hero.x, hero.y].ch:=hero.prev_cell;
  hero.x:=hero.x+mx;
  hero.y:=hero.y+my;
  level[hero.x, hero.y].ch:=hero.face;
  draw_map(level, rooms, hero);
  if check_the_ground(items, hero) then hero.prev_cell:=clip
                                   else hero.prev_cell:=pass;
  draw_map(level, rooms, hero);
  exit
end; // item pile
if level[hero.x+mx, hero.y+my].ch in ['a'..'z', 'A'..'Z'] then begin
  if do_damage(hero.x+mx, hero.y+my, monsters, hero, items, 'm') then begin
    level[hero.x+mx, hero.y+my].ch:=corpse;
    draw_map(level, rooms, hero)
  end;
  exit
end; // monster
if level[hero.x+mx, hero.y+my].ch=corpse then begin
  level[hero.x, hero.y].ch:=hero.prev_cell;
  hero.x:=hero.x+mx;
  hero.y:=hero.y+my;
  level[hero.x, hero.y].ch:=hero.face;
  hero.prev_cell:=corpse;
  draw_map(level, rooms, hero);
  auto_pickup(items, hero.x, hero.y, hero);
  for i:=0 to length(items)-1 do
    if (items[i].x=hero.x) and (items[i].y=hero.y) and
       (items[i].name<>'empty') then begin
      check_the_ground(items, hero);
      break
    end;
  exit
end // corpse
end; // move player

procedure move_monsters(var level : map; rooms : c_rooms; var hero : player;
                        var items : c_items; var monsters : c_monsters);
var i, j, dmg   : integer;
    x, y        : shortint;
    px, py      : shortint;
    direction   : shortint;
    the_room    : array [1..2] of TCoord;
    done        : boolean;
    msg         : string;
begin
the_room[1].X:=0; the_room[1].Y:=0; the_room[2].X:=0; the_room[2].Y:=0;
for i:=0 to length(rooms)-1 do
  if (hero.x in [rooms[i].x1+1..rooms[i].x2-1]) and
     (hero.y in [rooms[i].y1+1..rooms[i].y2-1]) then begin
    the_room[1].X:=rooms[i].x1+1; the_room[1].Y:=rooms[i].y1+1;
    the_room[2].X:=rooms[i].x2-1; the_room[2].Y:=rooms[i].y2-1;
    break
  end;
for i:=0 to length(monsters)-1 do begin
  if monsters[i].state=dead then continue;
  if monsters[i].blind_ctr<blindDuration then begin
    inc(monsters[i].blind_ctr); continue
  end;
  done:=false;
  if ((monsters[i].x>=the_room[1].X) and (monsters[i].x<=the_room[2].X) and
      (monsters[i].y>=the_room[1].Y) and (monsters[i].y<=the_room[2].Y)) or
       monsters[i].pursuit then begin
    if not monsters[i].pursuit then begin
      monsters[i].pursuit:=true;
      if monsters[i].name=monster_types[maxMonsters].name then
        display_messages('"You shall not pass!"');
      continue
    end;
    for j:=1 to maxGuns do
      if monsters[i].firearm.name=guns[j].name then begin // monster armed with a gun
        if connect_xy(monsters[i].x, monsters[i].y, hero.x, hero.y, level,
                      monsters, hero, items, 'c', x, y) then begin // clear shot
          dmg:=monster_damage(monsters[i], hero, 'r');
          if dmg<0 then dmg:=0;
          msg:=capitalize(monsters[i].name)+' shoots you for '+
                          inttostr(dmg)+' damage.';
          display_messages(msg);
          hero.life:=hero.life-dmg;
          SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_INTENSITY);
          gotoxy(monsters[i].x, monsters[i].y); write(monsters[i].face);
          sleep(fxDelay); SetConsoleTextAttribute(ConHandle, WHITE);
          if hero.life<=0 then begin
            display_messages('You die...');
            exit
          end;
          done:=true
        end;
        break
      end; // guncheck
    if done then continue;
    for j:=1 to maxTools do
      if monsters[i].tool.name=tools[j].name then begin // armed with a melee weapon
        for y:=monsters[i].y-1 to monsters[i].y+1 do
          for x:=monsters[i].x-1 to monsters[i].x+1 do
            if level[x, y].ch=hero.face then begin // to the face
              dmg:=monster_damage(monsters[i], hero, 'm');
              if dmg<0 then dmg:=0;
              msg:=capitalize(monsters[i].name)+' hits you for '+
                   inttostr(dmg)+' damage.';
              display_messages(msg);
              hero.life:=hero.life-dmg;
              SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or
                                      FOREGROUND_INTENSITY);
              gotoxy(monsters[i].x, monsters[i].y); write(monsters[i].face);
              sleep(fxDelay); SetConsoleTextAttribute(ConHandle, WHITE);
              if hero.life<=0 then begin
                display_messages('You die...');
                exit
              end;
              done:=true; break
            end;
        break
      end; // melee check
    if done then continue;
    if connect_xy(monsters[i].x, monsters[i].y, hero.x, hero.y, level,
                  monsters, hero, items, 'm', x, y) then
      connect_xy(monsters[i].x, monsters[i].y, hero.x, hero.y, level,
                 monsters, hero, items, 'p', x, y) else
      pathfind(monsters[i].x, monsters[i].y, hero.x, hero.y, level, x, y);
    if (level[x, y].ch=pass) or (level[x, y].ch=corpse) or
       (level[x, y].ch=clip) then begin // move towards player
      level[monsters[i].x, monsters[i].y].ch:=monsters[i].prev_cell;
      monsters[i].prev_cell:=level[x, y].ch;
      level[x, y].ch:=monsters[i].face;
      monsters[i].x:=x; monsters[i].y:=y
    end;
    done:=true
  end; // aggro
  if done then continue;
  direction:=random(9)+1;
  x:=0; y:=0;
  case direction of
       4 : x:=x-1;
       6 : x:=x+1;
       8 : y:=y-1;
       2 : y:=y+1;
       1 : begin x:=x-1; y:=y+1 end;
       7 : begin x:=x-1; y:=y-1 end;
       3 : begin x:=x+1; y:=y+1 end;
       9 : begin x:=x+1; y:=y-1 end
  end;
  if (level[monsters[i].x+x, monsters[i].y+y].ch=pass) or
     (level[monsters[i].x+x, monsters[i].y+y].ch=corpse) or
     (level[monsters[i].x+x, monsters[i].y+y].ch=clip) then begin
    level[monsters[i].x, monsters[i].y].ch:=monsters[i].prev_cell;
    monsters[i].prev_cell:=level[monsters[i].x+x, monsters[i].y+y].ch;
    level[monsters[i].x+x, monsters[i].y+y].ch:=monsters[i].face;
    monsters[i].x:=monsters[i].x+x;
    monsters[i].y:=monsters[i].y+y
  end // prowler
end; // monster action
draw_map(level, rooms, hero)
end; // move monsters

procedure blind_monsters(level : map; rooms : c_rooms;
                         var monsters : c_monsters; hero : player);
var i, j, k  : integer;
    the_room : array [1..2] of TCoord;
begin
the_room[1].X:=0; the_room[1].Y:=0; the_room[2].X:=0; the_room[2].Y:=0;
for i:=0 to length(rooms)-1 do
  if (hero.x in [rooms[i].x1+1..rooms[i].x2-1]) and
     (hero.y in [rooms[i].y1+1..rooms[i].y2-1]) then begin
    the_room[1].X:=rooms[i].x1+1; the_room[1].Y:=rooms[i].y1+1;
    the_room[2].X:=rooms[i].x2-1; the_room[2].Y:=rooms[i].y2-1;
    break
  end;
if the_room[1].X=0 then // used in a corridor = only works for adjacent cells
  for i:=hero.x-1 to hero.x+1 do
    for j:=hero.y-1 to hero.y+1 do
      for k:=0 to length(monsters)-1 do begin
        if (monsters[k].x=i) and (monsters[k].y=j) and
           (monsters[k].state=alive) then monsters[k].blind_ctr:=0;
        if level[i, j].ch<>wall then begin
          gotoxy(i, j); write(flash)
        end
      end;
if the_room[1].X>0 then begin
  for i:=0 to length(monsters)-1 do
    if (monsters[i].x in [the_room[1].X..the_room[2].X]) and
       (monsters[i].y in [the_room[1].Y..the_room[2].Y]) and
       (monsters[i].state=alive) then monsters[i].blind_ctr:=0;
  for i:=the_room[1].X to the_room[2].X do
    for j:=the_room[1].Y to the_room[2].Y do begin
      gotoxy(i, j); write(flash)
    end
end;
sleep(fxDelay*4); gotoxy(hero.x, hero.y)
end; // blind monsters

function use_medpack(var hero : player; var medpack_charge : integer) : boolean;
var pressed : boolean;
    keycode : word;
begin
result:=false;
gotoxy(30, 22); write('Use medpack? (');
SetConsoleTextAttribute(ConHandle, YELLOW); write('y');
SetConsoleTextAttribute(ConHandle, WHITE); write('/');
SetConsoleTextAttribute(ConHandle, YELLOW); write('n');
SetConsoleTextAttribute(ConHandle, WHITE); write(')');
pressed:=false;
while not pressed do begin
  keycode:=readkey;
  case keycode of
       $59 : begin // Y
               if medpack_charge>=medpackHealAmt then begin
                 result:=true;
                 hero.life:=hero.life+medpackHealAmt;
                 medpack_charge:=medpack_charge-medpackHealAmt;
                 SetConsoleTextAttribute(ConHandle, FOREGROUND_BLUE or
                                                    FOREGROUND_INTENSITY);
                 gotoxy(hero.x, hero.y); write(hero.face); sleep(fxDelay*4);
                 SetConsoleTextAttribute(ConHandle, WHITE);
                 clear_messages;
                 gotoxy(30, 22); write('Gained ', medpackHealAmt, ' life.')
               end else begin
                 clear_messages;
                 gotoxy(30, 22); write('No meds left.')
               end;
               pressed:=true
             end;
       $4e : begin // N
               clear_messages;
               pressed:=true
             end
  else continue
  end
end
end; // use medpack

procedure do_warp(var hero : player; var level : map; x1, y1, x2, y2 : byte;
                  var warp_counter : integer);
var pressed : boolean;
    keycode : word;
    x, y    : byte;
begin
gotoxy(30, 22); write('Use warp device? (');
SetConsoleTextAttribute(ConHandle, YELLOW); write('y');
SetConsoleTextAttribute(ConHandle, WHITE); write('/');
SetConsoleTextAttribute(ConHandle, YELLOW); write('n');
SetConsoleTextAttribute(ConHandle, WHITE); write(')');
pressed:=false;
while not pressed do begin
  keycode:=readkey;
  case keycode of
       $59 : begin // Y
               if warp_counter>=warpDelay then begin
                 repeat
                   x:=random(maxX-1)+1;
                   y:=random(maxY-1)+1
                 until (level[x, y].ch=pass) and
                       not ((x in [x1..x2]) and (y in [y1..y2]));
                 level[hero.x, hero.y].ch:=hero.prev_cell;
                 SetConsoleTextAttribute(ConHandle, FOREGROUND_GREEN or
                                                    FOREGROUND_INTENSITY);
                 gotoxy(hero.x, hero.y); write(hero.face); sleep(fxDelay*4);
                 SetConsoleTextAttribute(ConHandle, WHITE);
                 hero.x:=x;
                 hero.y:=y;
                 hero.prev_cell:=pass;
                 level[hero.x, hero.y].ch:=hero.face;
                 warp_counter:=0;
                 clear_messages
               end else begin
                 clear_messages;
                 gotoxy(30, 22); write('Still recharging.')
               end;
               pressed:=true
             end;
       $4e : begin // N
               clear_messages;
               pressed:=true
             end
  else continue
  end
end
end; // warp to a random spot on map

procedure status_update(hero : player; depth : byte; medpack_charge,
                        warp_counter : integer; win_condition : boolean);
var keycode : word;
    pressed : boolean;
begin
gotoxy(0, 22); write('Life: ', hero.life, '  ');
gotoxy(0, 23); write('Armor: ', hero.armor);
gotoxy(0, 24); write(hero.firearm.name, ' (', hero.ammo, '), ', hero.tool.name, ' ');
gotoxy(71, 22);
if (maxDepth+1-depth)<10 then write('Depth: ', maxDepth+1-depth, ' ')
                         else write('Depth: ', maxDepth+1-depth);
if medpack_charge>medpackHealAmt then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_INTENSITY);
if medpack_charge=medpackHealAmt then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_RED);
if medpack_charge<medpackHealAmt then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_INTENSITY);
gotoxy(71, 23); write('Medpack');
if warp_counter>=warpDelay then
  SetConsoleTextAttribute(ConHandle, FOREGROUND_GREEN or FOREGROUND_INTENSITY)
else
  SetConsoleTextAttribute(ConHandle, FOREGROUND_INTENSITY);
gotoxy(71, 24); write('Warp');
SetConsoleTextAttribute(ConHandle, WHITE);
pressed:=false;
if hero.life<=0 then begin
  SetConsoleTextAttribute(ConHandle, FOREGROUND_RED or FOREGROUND_INTENSITY);
  gotoxy(hero.x, hero.y); write(corpse);
  SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
  clear_frame(33, 8, 45, 12);
  draw_frame(34, 9, 44, 11, '', '');
  gotoxy(36, 10);
  SetConsoleTextAttribute(ConHandle, BACKGROUND_RED or BACKGROUND_GREEN or
                                     BACKGROUND_BLUE or BACKGROUND_INTENSITY);
  write(' R I P ');
  clear_frame(30, 14, 48, 16);
  SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
  gotoxy(31, 15); write('Press ');
  SetConsoleTextAttribute(ConHandle, YELLOW);
  write('Esc ');
  SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
  write('to exit');
  while not pressed do begin
    keycode:=readkey;
    if keycode=VK_ESCAPE then pressed:=true
  end
end;
if win_condition then begin
  clear_frame(30, 8, 47, 12);
  draw_frame(31, 9, 49, 11, '', '');
  gotoxy(33, 10);
{  SetConsoleTextAttribute(ConHandle, FOREGROUND_BLUE or FOREGROUND_INTENSITY or
                                     BACKGROUND_RED or BACKGROUND_GREEN or
                                     BACKGROUND_INTENSITY);}
  SetConsoleTextAttribute(ConHandle, BACKGROUND_RED or BACKGROUND_GREEN or
                                     BACKGROUND_BLUE or BACKGROUND_INTENSITY);
  write(' V I C T O R Y ');
  clear_frame(31, 14, 49, 16);
  SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
  gotoxy(32, 15); write('Press ');
  SetConsoleTextAttribute(ConHandle, YELLOW);
  write('Esc ');
  SetConsoleTextAttribute(ConHandle, WHITE or FOREGROUND_INTENSITY);
  write('to exit');
  while not pressed do begin
    keycode:=readkey;
    if keycode=VK_ESCAPE then pressed:=true
  end
end
end; // status update

procedure show_controls;
var pressed : boolean;
    keycode : word;
begin
draw_frame(5, 5, 75, 16, 'Key controls', '');
gotoxy(7, 6); SetConsoleTextAttribute(ConHandle, YELLOW);
write('Arrows'); SetConsoleTextAttribute(ConHandle, WHITE);
write(', '); SetConsoleTextAttribute(ConHandle, YELLOW);
write('Home'); SetConsoleTextAttribute(ConHandle, WHITE);
write(', '); SetConsoleTextAttribute(ConHandle, YELLOW);
write('End'); SetConsoleTextAttribute(ConHandle, WHITE);
write(', '); SetConsoleTextAttribute(ConHandle, YELLOW);
write('PgUp'); SetConsoleTextAttribute(ConHandle, WHITE);
write(', '); SetConsoleTextAttribute(ConHandle, YELLOW);
write('PgDn'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' or '); SetConsoleTextAttribute(ConHandle, YELLOW);
write('NumPad 1'); SetConsoleTextAttribute(ConHandle, WHITE);
write('-'); SetConsoleTextAttribute(ConHandle, YELLOW);
write('9'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = move around'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 7); write('.'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' or '); SetConsoleTextAttribute(ConHandle, YELLOW);
write('NumPad 5'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = skip a turn'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 8); write('F'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = fire weapon'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 9); write('L'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = inspect a cell'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 10); write('I'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = show inventory'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 11); write('M'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = use medpack'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 12); write('W'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = use warp device'); SetConsoleTextAttribute(ConHandle, YELLOW);
gotoxy(7, 13); write('Shift'); SetConsoleTextAttribute(ConHandle, WHITE);
write('+'); SetConsoleTextAttribute(ConHandle, YELLOW);
write('Q'); SetConsoleTextAttribute(ConHandle, WHITE);
write(' = quit game');
gotoxy(7, 15); write('Get to floor 1 and escape!');
pressed:=false;
while not pressed do begin
  keycode:=readkey;
  if keycode=VK_ESCAPE then pressed:=true
end
end; // show controls

var IBuff          : TInputRecord;
    IEvent         : DWord;
    level          : map;
    depth          : byte;
    c_depth        : byte;
    hero           : player;
    rooms          : c_rooms;
    monsters       : c_monsters;
    items          : c_items;
    i, j           : byte;
    direction      : byte;
    win_condition  : boolean;
    did_warp       : boolean;
    flash_bang     : boolean;
    warp_counter   : integer;
    medpack_charge : integer;

begin
ForceCurrentDirectory:=true;
ConHandle:=GetConOutputHandle;
coord:=GetLargestConsoleWindowSize(ConHandle);
SetConsoleCtrlHandler(@ConProc, false);
SetConsoleTitle('rogue-like prototype');
win_condition:=false;
hero.face:='@';
hero.name:='Eddy Pasterino';
hero.life:=100;
hero.armor:=0;
hero.firearm:=guns[1];
hero.tool:=tools[1];
hero.ammo:=ammo_types[1].quantity;
hero.state:=alive;
hero.prev_cell:=pass;
depth:=1;
warp_counter:=warpDelay;
flash_bang:=false;
medpack_charge:=medpackCapacity;
randomize;
new_instance(level, rooms, hero, monsters, items, depth);
gotoxy(30, 22); write('Press ');
SetConsoleTextAttribute(ConHandle, YELLOW); write('?');
SetConsoleTextAttribute(ConHandle, WHITE); write(' to see controls.');
while true do begin
  status_update(hero, depth, medpack_charge, warp_counter, win_condition);
  if (hero.life<=0) or win_condition then break;
  gotoxy(hero.x, hero.y);
  direction:=0;
  ReadConsoleInput(GetConInputHandle, IBuff, 1, IEvent);
  case IBuff.EventType of
       KEY_EVENT : begin
                     if IBuff.Event.KeyEvent.bKeyDown then clear_messages;
                     for i:=0 to 2 do msg_array[i]:='';
                     // shift+Q = quit game
                     if (IBuff.Event.KeyEvent.bKeyDown and
                        ((IBuff.Event.KeyEvent.wVirtualKeyCode=$51) and
                          (GetKeyState(VK_SHIFT)<0)))
                     then break;
                     // left arrow, num 4 = move left
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_LEFT) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$64)))
                     then direction:=4;
                     // right arrow, num 6 = move right
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_RIGHT) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$66)))
                     then direction:=6;
                     // up arrow, num 8 = move up
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_UP) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$68)))
                     then direction:=8;
                     // down arrow, num 2 = move down
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_DOWN) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$62)))
                     then direction:=2;
                     // PgUp, num 9 = move up+right
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_PRIOR) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$69)))
                     then direction:=9;
                     // PgDn, num 3 = move down+right
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_NEXT) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$63)))
                     then direction:=3;
                     // Home, num 7 = move up+left
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_HOME) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$67)))
                     then direction:=7;
                     // End, num 1 = move down+left
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=VK_END) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$61)))
                     then direction:=1;
                     // . , num 5 = skip a turn / wait
                     if ((IBuff.Event.KeyEvent.bKeyDown) and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=$be) or
                          (IBuff.Event.KeyEvent.wVirtualKeyCode=$65)))
                     then direction:=5;
                     // I = show inventory items
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$49) then begin
                       if show_inventory(items, hero, flash_bang)
                       then direction:=5;
                       if flash_bang then begin
                         draw_map(level, rooms, hero);
                         blind_monsters(level, rooms, monsters, hero)
                       end;
                       flash_bang:=false;
                       draw_map(level, rooms, hero)
                     end;
                     // F = fire weapon
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$46) then begin
                       if select_target(level, rooms, monsters, hero, items, 'f')
                       then direction:=5;
                       draw_map(level, rooms, hero)
                     end;
                     // L = look around (inspect tile)
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$4c) then begin
                       select_target(level, rooms, monsters, hero, items, 'i');
                       draw_map(level, rooms, hero)
                     end;
                     // ? = show control keys
                     if (IBuff.Event.KeyEvent.bKeyDown and
                         ((IBuff.Event.KeyEvent.wVirtualKeyCode=$bf) and
                          (GetKeyState(VK_SHIFT)<0))) then begin
                       show_controls;
                       draw_map(level, rooms, hero)
                     end;
                     // M = use medpack
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$4d) then begin
                       if use_medpack(hero, medpack_charge) then direction:=5
                     end;
                     // W = warp device
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$57) then begin
                       did_warp:=false;
                       for i:=0 to length(rooms)-1 do
                         if (hero.x in [rooms[i].x1..rooms[i].x2]) and
                            (hero.y in [rooms[i].y1..rooms[i].y2]) then begin
                           do_warp(hero, level, rooms[i].x1+1, rooms[i].y1+1,
                                   rooms[i].x2-1, rooms[i].y2-1, warp_counter);
                           did_warp:=true; break
                         end;
                       if not did_warp then do_warp(hero, level, hero.x, hero.y,
                                                    hero.x, hero.y, warp_counter);
                       draw_map(level, rooms, hero)
                     end;
{                     // N = skip to next level
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$4e) then begin
                       inc(depth);
                       new_instance(level, rooms, hero, monsters, items, depth)
                     end;
                     // R = reveal map
                     if (IBuff.Event.KeyEvent.bKeyDown) and
                        (IBuff.Event.KeyEvent.wVirtualKeyCode=$52) then begin
                       for i:=0 to maxY do
                         for j:=0 to maxX do
                           level[j, i].revealed:=true;
                       draw_map(level, rooms, hero)
                     end;}
                   end // KEY_EVENT
  else continue
  end; // case
  c_depth:=depth;
  if (direction>0) and (direction<10)  and (direction<>5) then begin
    if warp_counter<warpDelay then inc(warp_counter);
    win_condition:=move_player(level, rooms, hero, items, monsters, depth, direction)
  end;
  if (direction>0) and (direction<10) and (c_depth=depth) then
    move_monsters(level, rooms, hero, items, monsters)
end // read console input
end.
