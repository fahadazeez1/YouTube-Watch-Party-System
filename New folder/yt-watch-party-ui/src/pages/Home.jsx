import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { PlaySquare, Users } from 'lucide-react';

export default function Home() {
    const [mode, setMode] = useState('join'); // 'join' or 'host'
    const [username, setUsername] = useState('');
    const [roomIdInput, setRoomIdInput] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!username) {
            setError('Username is required');
            return;
        }

        try {
            if (mode === 'host') {
                const response = await axios.post(`https://web3task-ytsys.onrender.com/api/rooms/create?username=${username}`);
                const roomData = response.data;
                
                navigate(`/room/${roomData.id}`, { 
                    state: { 
                        userId: roomData.participants[0].id, 
                        role: 'HOST', 
                        username: username 
                    } 
                });
            } else {
                if (!roomIdInput) {
                    setError('Room ID is required to join');
                    return;
                }
                const response = await axios.post(`https://web3task-ytsys.onrender.com/api/rooms/${roomIdInput}/join?username=${username}`);
                const participantData = response.data;

                navigate(`/room/${roomIdInput}`, { 
                    state: { 
                        userId: participantData.id, 
                        role: 'PARTICIPANT', 
                        username: username 
                    } 
                });
            }
        } catch (err) {
            setError(err.response?.status === 404 ? 'Room not found. Check the ID.' : 'Server connection error.');
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-900">
            <div className="bg-gray-800 p-8 rounded-xl shadow-2xl w-full max-w-md border border-gray-700">
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold text-white mb-2 flex items-center justify-center gap-2">
                        <PlaySquare className="text-red-500" />
                        Watch Party
                    </h1>
                    <p className="text-gray-400">Sync YouTube videos with friends in real-time.</p>
                </div>

                <div className="flex gap-4 mb-6">
                    <button 
                        onClick={() => setMode('join')}
                        className={`flex-1 py-2 rounded-md font-semibold transition-colors ${mode === 'join' ? 'bg-red-600 text-white' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'}`}
                    >
                        Join Room
                    </button>
                    <button 
                        onClick={() => setMode('host')}
                        className={`flex-1 py-2 rounded-md font-semibold transition-colors ${mode === 'host' ? 'bg-red-600 text-white' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'}`}
                    >
                        Host Room
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-400 mb-1">Your Name</label>
                        <input 
                            type="text" 
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full bg-gray-700 border border-gray-600 rounded-md px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                            placeholder="Enter your display name"
                        />
                    </div>

                    {mode === 'join' && (
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-1">Room ID</label>
                            <input 
                                type="text" 
                                value={roomIdInput}
                                onChange={(e) => setRoomIdInput(e.target.value)}
                                className="w-full bg-gray-700 border border-gray-600 rounded-md px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-red-500"
                                placeholder="Paste Room ID here"
                            />
                        </div>
                    )}

                    {error && <div className="text-red-400 text-sm text-center">{error}</div>}

                    <button 
                        type="submit" 
                        className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3 rounded-md transition-colors mt-4 flex justify-center items-center gap-2"
                    >
                        {mode === 'join' ? <Users size={20} /> : <PlaySquare size={20} />}
                        {mode === 'join' ? 'Join Party' : 'Create Party'}
                    </button>
                </form>
            </div>
        </div>
    );
}