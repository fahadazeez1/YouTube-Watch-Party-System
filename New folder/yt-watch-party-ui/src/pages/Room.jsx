import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import YouTube from 'react-youtube';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import axios from 'axios';
import { Copy, LogOut, ShieldAlert, User, Play, Pause, Shield, Users, UserMinus } from 'lucide-react';

export default function Room() {
    const { roomId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const user = location.state;

    const [stompClient, setStompClient] = useState(null);
    const [videoId, setVideoId] = useState('dQw4w9WgXcQ');
    const [newVideoUrl, setNewVideoUrl] = useState('');
    const [participants, setParticipants] = useState([]);
    
    const [myRole, setMyRole] = useState(user?.role || 'PARTICIPANT');
    
    const playerRef = useRef(null);
    const isRemoteAction = useRef(false);
    const lastTimeRef = useRef(0);

    const fetchRoomData = useCallback(async () => {
        try {
            const response = await axios.get(`https://web3task-ytsys.onrender.com/api/rooms/${roomId}`);
            setVideoId(response.data.currentVideoId);
            setParticipants(response.data.participants);
        } catch (err) {
            console.error("Failed to load room data");
        }
    }, [roomId]);

    const sendLeaveRequest = useCallback(() => {
        if (!user || !user.userId) return;
        fetch(`https://web3task-ytsys.onrender.com/api/rooms/${roomId}/leave?userId=${user.userId}`, {
            method: 'POST',
            keepalive: true
        }).catch(err => console.error("Error leaving room", err));
    }, [roomId, user]);

    useEffect(() => {
        if (!user) {
            navigate('/');
            return;
        }

        fetchRoomData();

        const socket = new SockJS('https://web3task-ytsys.onrender.com/ws');
        const client = new Client({
            webSocketFactory: () => socket,
            onConnect: () => {
                client.subscribe(`/topic/room/${roomId}`, (message) => {
                    const data = JSON.parse(message.body);
                    handleRemoteSync(data);
                });

                client.publish({
                    destination: `/app/room/${roomId}/sync`,
                    body: JSON.stringify({ type: 'USER_JOINED', senderId: user.userId })
                });
            }
        });

        client.activate();
        setStompClient(client);

        window.addEventListener('beforeunload', sendLeaveRequest);

        return () => {
            sendLeaveRequest(); 
            if (client) client.deactivate();
            window.removeEventListener('beforeunload', sendLeaveRequest);
        };
    }, [roomId, user, navigate, fetchRoomData, sendLeaveRequest]);

    const handleRemoteSync = (data) => {
        if (data.type === 'USER_JOINED') {
            fetchRoomData(); 
            return;
        }

        if (data.type === 'ASSIGN_ROLE') {
            fetchRoomData();
            if (data.targetUserId === user.userId) {
                setMyRole(data.newRole);
                alert(`You have been promoted to ${data.newRole}!`);
            }
            return;
        }

        if (data.type === 'LEAVE_ROOM') {
            setParticipants(prev => prev.filter(p => p.id !== data.senderId));
            return;
        }

        if (data.type === 'REMOVE_PARTICIPANT') {
            if (data.targetUserId === user.userId) {
                alert("You have been removed from the room by the Host.");
                navigate('/');
            } else {
                setParticipants(prev => prev.filter(p => p.id !== data.targetUserId));
            }
            return;
        }

        if (data.senderId === user.userId) return;

        isRemoteAction.current = true;

        if ((data.type === 'PLAY' || data.type === 'SEEK') && playerRef.current) {
            playerRef.current.seekTo(data.currentTime);
            playerRef.current.playVideo();
        } else if (data.type === 'PAUSE' && playerRef.current) {
            playerRef.current.pauseVideo();
            playerRef.current.seekTo(data.currentTime);
        } else if (data.type === 'CHANGE_VIDEO') {
            setVideoId(data.videoId);
        }

        setTimeout(() => { isRemoteAction.current = false; }, 500);
    };

    const broadcastEvent = (eventType, time = 0, vId = null, targetUserId = null, newRole = null) => {
        if (myRole === 'PARTICIPANT') return; 

        if (stompClient && stompClient.connected) {
            stompClient.publish({
                destination: `/app/room/${roomId}/sync`,
                body: JSON.stringify({
                    type: eventType,
                    currentTime: time,
                    videoId: vId,
                    senderId: user.userId,
                    targetUserId: targetUserId,
                    newRole: newRole
                })
            });
        }
    };

    const handleLeaveClick = () => {
        navigate('/');
    };

    const onPlayerReady = (event) => { playerRef.current = event.target; };
    const onPlay = () => { if (!isRemoteAction.current) broadcastEvent('PLAY', playerRef.current.getCurrentTime()); };
    const onPause = () => { if (!isRemoteAction.current) broadcastEvent('PAUSE', playerRef.current.getCurrentTime()); };

    const onStateChange = (event) => {
        if (!playerRef.current || isRemoteAction.current) return;
        if (event.data === 3 && (myRole === 'HOST' || myRole === 'MODERATOR')) {
            const newTime = playerRef.current.getCurrentTime();
            if (Math.abs(newTime - lastTimeRef.current) > 1) {
                broadcastEvent('SEEK', newTime);
            }
        }
        lastTimeRef.current = playerRef.current.getCurrentTime() || 0;
    };

    const handleHostPlay = () => { if (playerRef.current) playerRef.current.playVideo(); };
    const handleHostPause = () => { if (playerRef.current) playerRef.current.pauseVideo(); };

    const handleChangeVideo = (e) => {
        e.preventDefault();
        const urlParams = new URLSearchParams(new URL(newVideoUrl).search);
        const newId = urlParams.get('v') || newVideoUrl;
        setVideoId(newId);
        broadcastEvent('CHANGE_VIDEO', 0, newId);
        setNewVideoUrl('');
    };

    const promoteToModerator = (targetParticipantId) => {
        broadcastEvent('ASSIGN_ROLE', 0, null, targetParticipantId, 'MODERATOR');
    };

    const kickParticipant = (targetParticipantId) => {
        if(window.confirm("Are you sure you want to remove this user?")) {
            broadcastEvent('REMOVE_PARTICIPANT', 0, null, targetParticipantId, null);
        }
    };

    if (!user) return null;
    const hasControls = myRole === 'HOST' || myRole === 'MODERATOR';

    return (
        <div className="min-h-screen bg-gray-900 flex flex-col md:flex-row">
            <div className="flex-1 flex flex-col p-4 md:p-8">
                <div className="flex justify-between items-center mb-6">
                    <h1 className="text-2xl font-bold flex items-center gap-2 text-white">
                        <span className="bg-red-600 px-3 py-1 rounded-md text-sm">LIVE</span>
                        Watch Party
                    </h1>
                    <button onClick={handleLeaveClick} className="text-gray-400 hover:text-white flex items-center gap-2">
                        <LogOut size={18} /> Leave
                    </button>
                </div>

                <div className={`w-full aspect-video bg-black rounded-xl overflow-hidden shadow-2xl border border-gray-800 ${!hasControls ? 'pointer-events-none' : 'pointer-events-auto'}`}>
                    <YouTube
                        videoId={videoId}
                        opts={{ width: '100%', height: '100%', playerVars: { autoplay: 1 } }}
                        onReady={onPlayerReady}
                        onPlay={onPlay}
                        onPause={onPause}
                        onStateChange={onStateChange}
                        className="w-full h-full"
                    />
                </div>

                {hasControls && (
                    <div className="mt-8 bg-gray-800 p-6 rounded-xl border border-gray-700">
                        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-white">
                            <ShieldAlert className="text-yellow-500" /> Video Controls ({myRole})
                        </h3>
                        <div className="flex gap-4 mb-6">
                            <button onClick={handleHostPlay} className="flex-1 bg-green-600 hover:bg-green-700 text-white font-bold py-3 rounded-md flex items-center justify-center gap-2">
                                <Play size={20} /> Play Video
                            </button>
                            <button onClick={handleHostPause} className="flex-1 bg-yellow-600 hover:bg-yellow-700 text-white font-bold py-3 rounded-md flex items-center justify-center gap-2">
                                <Pause size={20} /> Pause Video
                            </button>
                        </div>
                        <form onSubmit={handleChangeVideo} className="flex gap-4">
                            <input 
                                type="text" value={newVideoUrl} onChange={(e) => setNewVideoUrl(e.target.value)}
                                placeholder="Paste new YouTube URL"
                                className="flex-1 bg-gray-700 border border-gray-600 rounded-md px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                            />
                            <button type="submit" className="bg-red-600 hover:bg-red-700 px-6 py-2 rounded-md font-semibold text-white">Change Video</button>
                        </form>
                    </div>
                )}
            </div>

            <div className="w-full md:w-80 bg-gray-800 border-l border-gray-700 p-6 flex flex-col">
                <div className="mb-8">
                    <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-2">Room ID</h2>
                    <div className="flex items-center gap-2 bg-gray-900 p-3 rounded-md border border-gray-700">
                        <code className="flex-1 text-sm text-green-400 truncate">{roomId}</code>
                        <button onClick={() => navigator.clipboard.writeText(roomId)} className="text-gray-400 hover:text-white">
                            <Copy size={18} />
                        </button>
                    </div>
                </div>

                <div className="flex-1">
                    <h2 className="text-sm font-semibold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-2">
                        {myRole === 'PARTICIPANT' ? <User size={18} /> : <Users size={18} />} 
                        {myRole === 'PARTICIPANT' ? 'My Status' : `Participants (${participants.length})`}
                    </h2>
                    
                    <div className="space-y-3">
                        {participants
                            .filter(p => myRole !== 'PARTICIPANT' || p.id === user.userId)
                            .map((p) => (
                            <div key={p.id} className="bg-gray-700/50 p-3 rounded-lg flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className={`p-2 rounded-full ${p.role === 'HOST' ? 'bg-yellow-500/20 text-yellow-500' : p.role === 'MODERATOR' ? 'bg-green-500/20 text-green-500' : 'bg-blue-500/20 text-blue-500'}`}>
                                        {p.role === 'HOST' ? <ShieldAlert size={16} /> : p.role === 'MODERATOR' ? <Shield size={16} /> : <User size={16} />}
                                    </div>
                                    <div>
                                        <p className="font-semibold text-sm text-white">
                                            {p.username} {p.id === user.userId && "(You)"}
                                        </p>
                                        <p className="text-xs text-gray-400">{p.role}</p>
                                    </div>
                                </div>
                                
                                {myRole === 'HOST' && p.id !== user.userId && (
                                    <div className="flex flex-col gap-1">
                                        {p.role === 'PARTICIPANT' && (
                                            <button onClick={() => promoteToModerator(p.id)} className="text-[10px] bg-gray-600 hover:bg-green-600 px-2 py-1 rounded text-white font-bold transition-colors">
                                                MAKE MOD
                                            </button>
                                        )}
                                        <button onClick={() => kickParticipant(p.id)} className="text-[10px] bg-gray-600 hover:bg-red-600 px-2 py-1 rounded text-white font-bold transition-colors flex items-center justify-center gap-1">
                                            <UserMinus size={10} /> KICK
                                        </button>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}