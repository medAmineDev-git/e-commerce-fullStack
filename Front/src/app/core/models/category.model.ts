export interface Category {
	id: number;
	name: string;
	description: string;
	parentId?: number | null;
	parentName?: string | null;
}

export type CategoryInput = Omit<Category, 'id'>;
