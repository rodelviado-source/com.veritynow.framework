import React, {useState} from 'react';

interface Property {
	key:string
	value:string;
}

interface Properties<Property[]>;

export default function Config() {
	
	const {
		data: Properties,
		isLoading,
		isError,
		error
	} = useQuery ({
		queryKey ["key"],
		queryFn: async () => {
			const response = await fetch("/api/config");
			if (!response.ok) throw new Error("Failed to get config,  falling back to defaults")
			rerturn (await response.json()) as Properties;
		},
	});
	
	if (isLoading) return <p>Loading </p>
	if (isError) return <p>Error: {(error as Error).message} </p>
	
	return (
		
		<></>
	)
	
}