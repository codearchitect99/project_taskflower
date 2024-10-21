import {getTagBGColor} from "../util/TagUtil";
import React, {useState} from "react";
import {TagColor} from "../api/tag/TagTypes";

interface ColorSelectBoxProps {
    list: TagColor[];
    state: TagColor | null;
    setState: (color: TagColor) => void;
}

export const ColorSelectBox: React.FC<ColorSelectBoxProps> = ({ list, state, setState }) => {
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    const handleSelect = (color: TagColor) => {
        setState(color);
        setIsDropdownOpen(false);
    };

    const toggleDropdown = () => {
        setIsDropdownOpen(!isDropdownOpen);
    };

    return (
        <div className="relative">
            <button
                type="button"
                onClick={toggleDropdown}
                className={`badge cursor-pointer ${getTagBGColor(state ? state.toString() : '')}`} />
            {isDropdownOpen && (
                <ul className="mb-1 rounded-box flex flex-col z-[1] shadow absolute bg-white">
                    {list.map((option, index) => (
                        <li
                            key={`TagColor-${option}-${index}`}
                            className={`badge cursor-pointer ${getTagBGColor(option.toString())} text-center`}
                            onClick={() => handleSelect(option)}/>
                    ))}
                </ul>
            )}
        </div>
    );
};
